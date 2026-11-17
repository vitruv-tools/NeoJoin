package tools.vitruv.neojoin.validation;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.validation.Check;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.Formatting;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.common.Pair;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.AstUtils;
import tools.vitruv.neojoin.utils.ImplicitClassCollector;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validates the names of the implicit target classes (classes that {@link tools.vitruv.neojoin.aqr.AQRBuilder} creates
 * for referenced source classes that no query selects, see {@link ImplicitClassCollector}) and the names of the
 * containment references of the root class.
 * <p>
 * An implicit class gets the name of its source class. {@code AQRBuilder.populateRoot} adds a containment reference
 * for each non-abstract target class except the root to the root class, named by
 * {@link Formatting#formatRootReferenceName}, and fails with "Duplicated feature name '...' in root class" if two
 * classes get the same reference name. Duplicated names of explicit target classes are reported by
 * {@link NeoJoinValidator#duplicatedTargetClassName}. The issues of an implicit class are reported at the feature that
 * references its source class, or at the query if the references are copied. The issues of a query are reported at
 * its name; for a query without name, Xtext marks the whole query.
 */
public class TargetClassNameValidator extends ComposableValidator {

    @Inject
    private ExpressionHelper expressionHelper;

    /**
     * Checks that the name of each implicit class is unique among the target classes, including the implicit root
     * class. An implicit class has the name of its source class, so the source class name, the name of the implicit
     * class and the name of a conflicting target class are the same.
     */
    @Check
    public void checkImplicitClassNames(ViewTypeDefinition viewType) {
        var implicitClasses = ImplicitClassCollector.collect(viewType, expressionHelper);
        var queriesByName = AstUtils.getAllQueries(viewType)
            .collect(Collectors.groupingBy(query -> AstUtils.getTargetName(query, expressionHelper)));
        var implicitClassesByName = implicitClasses.entrySet().stream()
            .collect(Collectors.groupingBy(
                implicitClass -> implicitClass.getKey().getName(),
                LinkedHashMap::new,
                Collectors.toList()
            ));
        var hasImplicitRoot = AstUtils.hasImplicitRoot(viewType);

        implicitClassesByName.forEach((name, implicitClassesWithName) -> {
            // several implicit classes can have the same cause (references copied by one query)
            var causes = implicitClassesWithName.stream().map(Map.Entry::getValue).distinct().toList();
            checkImplicitClassesWithSameName(viewType, name, implicitClassesWithName, causes);
            checkImplicitClassesWithNameOfQueries(name, causes, queriesByName.getOrDefault(name, List.of()));
            if (hasImplicitRoot && name.equals(Constants.DefaultRootClassName)) {
                causes.forEach(cause -> error(
                    ("Implicit target class of source class '%s' collides with name of the implicit root class. "
                        + "Either create a query for the source class that specifies a different target class name "
                        + "or explicitly create a root class with a different name.").formatted(name),
                    cause,
                    null
                ));
            }
        });
    }

    /**
     * Implicit classes of source classes with the same name from different meta-models would get the same name. The
     * source classes are named with the import alias of their package, which is unique
     * ({@link ImportValidator#checkDuplicatedImportNames}), or with the package name if the package is not imported
     * (a class referenced from another meta-model). Only if these names are not unique (packages with the same name
     * that are not imported), the namespace URIs are added at the end of the message.
     */
    private void checkImplicitClassesWithSameName(
        ViewTypeDefinition viewType,
        String name,
        List<Map.Entry<EClass, EObject>> implicitClasses,
        List<EObject> causes
    ) {
        if (implicitClasses.size() < 2) {
            return;
        }

        // qualified source class name and namespace URI, sorted by both
        var sourceClasses = implicitClasses.stream()
            .map(implicitClass -> implicitClass.getKey().getEPackage())
            .map(pack -> new Pair<>(getPackageQualifier(viewType, pack) + "." + name, pack.getNsURI()))
            .sorted(Comparator.<Pair<String, String>, String>comparing(Pair::left).thenComparing(Pair::right))
            .toList();
        var sourceClassNames = sourceClasses.stream().map(Pair::left).toList();
        var namespaceURIs = sourceClassNames.stream().distinct().count() < sourceClassNames.size()
            ? " Namespace URIs: " + sourceClasses.stream().map(Pair::right).collect(Collectors.joining(", "))
            : "";
        causes.forEach(cause -> error(
            ("Implicit target classes of the source classes %s have the same name '%s'. "
                + "For all but one of these source classes, "
                + "create a query that specifies a different target class name.%s")
                .formatted(String.join(", ", sourceClassNames), name, namespaceURIs),
            cause,
            null
        ));
    }

    private static String getPackageQualifier(ViewTypeDefinition viewType, EPackage pack) {
        var alias = AstUtils.findImportAlias(viewType, pack);
        return alias != null ? alias : pack.getName();
    }

    /**
     * Reports implicit classes whose name is the target class name of queries, at the causes and at the queries.
     */
    private void checkImplicitClassesWithNameOfQueries(String name, List<EObject> causes, List<Query> queries) {
        if (queries.isEmpty()) {
            return;
        }

        causes.forEach(cause -> error(
            ("Implicit target class of source class '%s' has the same name as the target class '%s'. "
                + "Either choose a different name for the target class "
                + "or create a query for the source class that specifies a different target class name.")
                .formatted(name, name),
            cause,
            null
        ));
        queries.forEach(query -> error(
            ("Target class '%s' has the same name as the implicit target class of source class '%s'. "
                + "Either choose a different name for this class "
                + "or create a query for the source class that specifies a different target class name.")
                .formatted(name, name),
            query,
            AstPackage.Literals.QUERY__NAME
        ));
    }

    /**
     * Checks that the target classes that get a containment reference in the root class (see
     * {@link AstUtils#hasRootReference}) get different reference names. Names that differ only in the case of the
     * first letter get the same reference name (see {@link Formatting#formatRootReferenceName}). Classes with the same
     * name are reported by {@link #checkImplicitClassNames} and {@link NeoJoinValidator#duplicatedTargetClassName}.
     */
    @Check
    public void checkRootReferenceNames(ViewTypeDefinition viewType) {
        var implicitClasses = ImplicitClassCollector.collect(viewType, expressionHelper);
        var queriesByReferenceName = AstUtils.getAllQueries(viewType)
            .filter(query -> AstUtils.hasRootReference(query, expressionHelper))
            .collect(Collectors.groupingBy(
                query -> Formatting.formatRootReferenceName(AstUtils.getTargetName(query, expressionHelper))
            ));
        // implicit classes of abstract source classes are abstract
        var implicitClassesByReferenceName = implicitClasses.entrySet().stream()
            .filter(implicitClass -> !implicitClass.getKey().isAbstract())
            .collect(Collectors.groupingBy(
                implicitClass -> Formatting.formatRootReferenceName(implicitClass.getKey().getName())
            ));

        var referenceNames = new TreeSet<>(queriesByReferenceName.keySet());
        referenceNames.addAll(implicitClassesByReferenceName.keySet());
        for (var referenceName : referenceNames) {
            var queries = queriesByReferenceName.getOrDefault(referenceName, List.of());
            var implicitClassesWithName = implicitClassesByReferenceName.getOrDefault(referenceName, List.of());
            var classNames = Stream.concat(
                queries.stream().map(query -> AstUtils.getTargetName(query, expressionHelper)),
                implicitClassesWithName.stream().map(implicitClass -> implicitClass.getKey().getName())
            ).collect(Collectors.toCollection(TreeSet::new));
            if (classNames.size() < 2) {
                continue;
            }

            var message = ("Target classes %s have the same containment reference '%s' in the root class. "
                + "Choose names that differ in more than the case of the first letter.").formatted(
                classNames.stream().map(name -> "'" + name + "'").collect(Collectors.joining(", ")),
                referenceName
            );
            queries.forEach(query -> error(message, query, AstPackage.Literals.QUERY__NAME));
            implicitClassesWithName.stream().map(Map.Entry::getValue).distinct()
                .forEach(cause -> error(message, cause, null));
        }
    }

}
