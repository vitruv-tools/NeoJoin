package tools.vitruv.neojoin.validation;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.common.ClassifierNames;
import tools.vitruv.neojoin.common.ClassifierNames.WrittenName;
import tools.vitruv.neojoin.utils.AstUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Formats source classes, enumerations and data types for validation messages the way they are written in a query,
 * see {@link ClassifierNames} for the rules.
 * <p>
 * The candidates are the classes of the imported packages, or the data types of the imported packages and Ecore
 * ({@link AstUtils#getFeatureTypePackagesByAlias}), each including the classifiers of the packages that these
 * reference.
 */
final class MessageFormatting {

    private MessageFormatting() {}

    /**
     * Returns the wording for the implicit classes of the given number of source classes: "implicit class of source
     * class" or "implicit classes of the source classes".
     */
    static String implicitClassesOfSourceClasses(int count) {
        return count == 1 ? "implicit class of source class" : "implicit classes of the source classes";
    }

    /**
     * Returns the written name of one classifier (a source class, an enumeration or a data type), see
     * {@link MessageFormatting}.
     */
    static WrittenName formatClassifier(ViewTypeDefinition viewType, EClassifier classifier) {
        return getWrittenNames(viewType, List.of(classifier)).get(0);
    }

    /**
     * Formats source classes sorted by their written names and separated by commas. A single source class is
     * quoted, several are not.
     */
    static String formatSourceClasses(ViewTypeDefinition viewType, List<EClass> sourceClasses) {
        return join(getWrittenNames(viewType, sourceClasses));
    }

    /**
     * Formats data types (including enumerations) as "data type X" or "data types X, Y", sorted by their written
     * names. A single data type is quoted, several are not.
     */
    static String formatDataTypes(ViewTypeDefinition viewType, List<EDataType> dataTypes) {
        return (dataTypes.size() == 1 ? "data type " : "data types ") + join(getWrittenNames(viewType, dataTypes));
    }

    /**
     * Formats names (for example of features) separated by commas. A single name is quoted, several are not.
     */
    static String formatNames(List<String> names) {
        if (names.size() == 1) {
            return "'" + names.get(0) + "'";
        }
        return String.join(", ", names);
    }

    private static String join(List<WrittenName> writtenNames) {
        if (writtenNames.size() == 1) {
            return writtenNames.get(0).quoted();
        }
        return writtenNames.stream().map(WrittenName::plain).collect(Collectors.joining(", "));
    }

    /**
     * Returns the written names of the given classifiers (all of the same kind: classes, or data types), sorted by
     * name and namespace URI. The candidates are computed once.
     */
    private static List<WrittenName> getWrittenNames(ViewTypeDefinition viewType, List<? extends EClassifier> list) {
        var names = createClassifierNames(viewType, list.get(0) instanceof EClass);

        return list.stream()
            .map(names::write)
            .sorted(Comparator.comparing(WrittenName::name)
                .thenComparing(writtenName -> String.valueOf(writtenName.namespaceUri())))
            .toList();
    }

    /**
     * Returns the names for classes (the start packages are the imported packages) or for data types (the imported
     * packages and Ecore).
     */
    private static ClassifierNames createClassifierNames(ViewTypeDefinition viewType, boolean isClass) {
        var importedPackages = AstUtils.getImportedPackagesByAlias(viewType);
        var startPackages = isClass ? importedPackages : AstUtils.getFeatureTypePackagesByAlias(viewType);

        var aliasByPackage = new HashMap<EPackage, String>();
        importedPackages.forEach((alias, pack) -> aliasByPackage.putIfAbsent(pack, alias));
        return new ClassifierNames(aliasByPackage, startPackages.values());
    }

}
