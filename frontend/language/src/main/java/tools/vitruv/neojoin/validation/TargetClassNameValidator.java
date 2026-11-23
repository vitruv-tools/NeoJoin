package tools.vitruv.neojoin.validation;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
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
import tools.vitruv.neojoin.utils.ImplicitClassCache;
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
 * containment references of the root class, and checks that no target class has the name of a data type.
 * <p>
 * An implicit class gets the name of its source class. {@code AQRBuilder.populateRoot} adds a containment reference
 * for each non-abstract target class except the root to the root class, named by
 * {@link Formatting#formatRootReferenceName}, and fails with "Duplicated feature name '...' in root class" if two
 * classes get the same reference name. Duplicated names of explicit target classes are reported by
 * {@link NeoJoinValidator#duplicatedTargetClassName}. The issues of an implicit class are reported at the feature that
 * references its source class, or at the query if the references are copied. The issues of a query are reported at
 * its name; for a query without name, Xtext marks the whole query.
 * <p>
 * A target class must not have the name of a data type that can be used as explicit feature type (see
 * {@link #checkQueryNamesAgainstDataTypes} and {@link #checkImplicitClassNamesAgainstDataTypes}).
 */
public class TargetClassNameValidator extends ComposableValidator {

    @Inject
    private ExpressionHelper expressionHelper;

    @Inject
    private ImplicitClassCache implicitClassCache;

    /**
     * Checks that the name of each implicit class is unique among the target classes, including the implicit root
     * class. An implicit class has the name of its source class, so the source class name, the name of the implicit
     * class and the name of a conflicting target class are the same.
     */
    @Check
    public void checkImplicitClassNames(ViewTypeDefinition viewType) {
        var implicitClasses = implicitClassCache.getImplicitClasses(viewType);
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
            checkImplicitClassesWithNameOfQueries(
                viewType,
                name,
                implicitClassesWithName,
                queriesByName.getOrDefault(name, List.of())
            );
            if (hasImplicitRoot && name.equals(Constants.DefaultRootClassName)) {
                // one issue per implicit class, so that each names exactly one source class
                implicitClassesWithName.forEach(implicitClass -> {
                    var sourceClass = formatSourceClasses(viewType, name, List.of(implicitClass));
                    error(
                        ("Implicit target class of source class %s collides with name of the implicit root class. "
                            + "Either create a query for the source class that specifies a different target class "
                            + "name or explicitly create a root class with a different name.%s")
                            .formatted(sourceClass.left(), sourceClass.right()),
                        implicitClass.getValue(),
                        null
                    );
                });
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

        var sourceClasses = formatSourceClasses(viewType, name, implicitClasses);
        causes.forEach(cause -> error(
            ("Implicit target classes of the source classes %s have the same name '%s'. "
                + "For all but one of these source classes, "
                + "create a query that specifies a different target class name.%s")
                .formatted(sourceClasses.left(), name, sourceClasses.right()),
            cause,
            null
        ));
    }

    private static String getPackageQualifier(ViewTypeDefinition viewType, EPackage pack) {
        var alias = AstUtils.findImportAlias(viewType, pack);
        return alias != null ? alias : pack.getName();
    }

    /**
     * Formats classes or data types for a message: each is named {@code <qualifier>.<name>} (see
     * {@link #getPackageQualifier}), sorted and separated by commas.
     *
     * @param packagesWithNames the package and the name of each class or data type
     * @return the formatted list and the suffix for the end of the message: the namespace URIs (sorted like the names)
     *     if the qualified names are not unique, otherwise empty
     */
    private static Pair<String, String> formatQualifiedNames(
        ViewTypeDefinition viewType,
        List<Pair<EPackage, String>> packagesWithNames
    ) {
        // qualified name and namespace URI, sorted by both
        var qualifiedNames = packagesWithNames.stream()
            .map(packageWithName -> new Pair<>(
                getPackageQualifier(viewType, packageWithName.left()) + "." + packageWithName.right(),
                packageWithName.left().getNsURI()
            ))
            .sorted(Comparator.<Pair<String, String>, String>comparing(Pair::left).thenComparing(Pair::right))
            .toList();
        var names = qualifiedNames.stream().map(Pair::left).toList();
        var namespaceURIs = names.stream().distinct().count() < names.size()
            ? " Namespace URIs: " + qualifiedNames.stream().map(Pair::right).collect(Collectors.joining(", "))
            : "";
        return new Pair<>(String.join(", ", names), namespaceURIs);
    }

    /**
     * Formats the source classes of the implicit classes, see {@link #formatQualifiedNames}.
     */
    private static Pair<String, String> formatSourceClasses(
        ViewTypeDefinition viewType,
        String name,
        List<Map.Entry<EClass, EObject>> implicitClasses
    ) {
        return formatQualifiedNames(
            viewType,
            implicitClasses.stream()
                .map(implicitClass -> new Pair<>(implicitClass.getKey().getEPackage(), name))
                .toList()
        );
    }

    /**
     * Reports implicit classes whose name is the target class name of queries, at the causes and at the queries.
     */
    private void checkImplicitClassesWithNameOfQueries(
        ViewTypeDefinition viewType,
        String name,
        List<Map.Entry<EClass, EObject>> implicitClasses,
        List<Query> queries
    ) {
        if (queries.isEmpty()) {
            return;
        }

        var allSourceClasses = formatSourceClasses(viewType, name, implicitClasses);
        var implicitClassesOfQuery = implicitClasses.size() == 1
            ? "the implicit target class of source class"
            : "the implicit target classes of the source classes";
        // one issue per implicit class, so that each names exactly one source class
        implicitClasses.forEach(implicitClass -> {
            var sourceClass = formatSourceClasses(viewType, name, List.of(implicitClass));
            error(
                ("Implicit target class of source class %s has the same name as the target class '%s'. "
                    + "Either choose a different name for the target class "
                    + "or create a query for the source class that specifies a different target class name.%s")
                    .formatted(sourceClass.left(), name, sourceClass.right()),
                implicitClass.getValue(),
                null
            );
        });
        queries.forEach(query -> error(
            ("Target class '%s' has the same name as %s %s. "
                + "Either choose a different name for this class "
                + "or create a query for the source class that specifies a different target class name.%s")
                .formatted(name, implicitClassesOfQuery, allSourceClasses.left(), allSourceClasses.right()),
            query,
            AstPackage.Literals.QUERY__NAME
        ));
    }

    /**
     * Checks that no target class of a query has the name of a data type of the imported packages or of Ecore, which
     * can be used as explicit feature type. The generated view type would contain a class and a data type with the
     * same name, because {@code MetaModelGenerator} copies the used data types into the view package (EMF reports
     * "There may not be two classifiers named ..."). An explicit feature type with this name would also be ambiguous,
     * because {@code NeoJoinScopeProvider.createFeatureTypeScope} prefers target classes over data types. The implicit
     * root class is not checked.
     */
    @Check
    public void checkQueryNamesAgainstDataTypes(ViewTypeDefinition viewType) {
        var dataTypesByName = AstUtils.getFeatureTypeDataTypes(viewType);
        AstUtils.getAllQueries(viewType).forEach(query -> {
            var name = AstUtils.getTargetName(query, expressionHelper);
            var dataTypes = dataTypesByName.get(name);
            if (dataTypes != null) {
                var formattedDataTypes = formatDataTypes(viewType, dataTypes);
                error(
                    "Target class '%s' has the same name as the %s. Choose a different name for this class.%s"
                        .formatted(name, formattedDataTypes.left(), formattedDataTypes.right()),
                    query,
                    AstPackage.Literals.QUERY__NAME
                );
            }
        });
    }

    /**
     * Checks that no implicit class has the name of a data type that can be used as explicit feature type, for the
     * reasons given in {@link #checkQueryNamesAgainstDataTypes}. The conflict is reported at the element that causes
     * the implicit class.
     */
    @Check
    public void checkImplicitClassNamesAgainstDataTypes(ViewTypeDefinition viewType) {
        var dataTypesByName = AstUtils.getFeatureTypeDataTypes(viewType);
        implicitClassCache.getImplicitClasses(viewType).forEach((sourceClass, cause) -> {
            var name = sourceClass.getName();
            var dataTypes = dataTypesByName.get(name);
            if (dataTypes != null) {
                var sourceClasses = formatSourceClasses(viewType, name, List.of(Map.entry(sourceClass, cause)));
                var formattedDataTypes = formatDataTypes(viewType, dataTypes);
                error(
                    ("Implicit target class of source class %s has the same name as the %s. "
                        + "Create a query for the source class that specifies a different target class name.%s")
                        .formatted(
                            sourceClasses.left(),
                            formattedDataTypes.left(),
                            formattedDataTypes.right()
                        ),
                    cause,
                    null
                );
            }
        });
    }

    /**
     * Formats data types with the same name as "the data type X" or "the data types X, Y", see
     * {@link #formatQualifiedNames}.
     */
    private static Pair<String, String> formatDataTypes(ViewTypeDefinition viewType, List<EDataType> dataTypes) {
        var formatted = formatQualifiedNames(
            viewType,
            dataTypes.stream().map(dataType -> new Pair<>(dataType.getEPackage(), dataType.getName())).toList()
        );
        return new Pair<>((dataTypes.size() == 1 ? "data type " : "data types ") + formatted.left(), formatted.right());
    }

    /**
     * Checks that the target classes that get a containment reference in the root class (see
     * {@link AstUtils#hasRootReference}) get different reference names. Names that differ only in the case of the
     * first letter get the same reference name (see {@link Formatting#formatRootReferenceName}). Classes with the same
     * name are reported by {@link #checkImplicitClassNames} and {@link NeoJoinValidator#duplicatedTargetClassName}.
     */
    @Check
    public void checkRootReferenceNames(ViewTypeDefinition viewType) {
        var implicitClasses = implicitClassCache.getImplicitClasses(viewType);
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
