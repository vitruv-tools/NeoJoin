package tools.vitruv.neojoin.validation;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.common.Pair;
import tools.vitruv.neojoin.utils.AstUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Formats source classes and data types for validation messages the way they are written in a query: with the simple
 * name, or with the qualified name (see {@link AstUtils#getQualifiedNameInQuery}) if the simple name is ambiguous.
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
     * Formats source classes, see {@link #formatClassifiers}. A simple name is ambiguous if another class of the
     * imported packages has it.
     *
     * @return the formatted list and the suffix for the end of the message, see {@link #formatClassifiers}
     */
    static Pair<String, String> formatSourceClasses(ViewTypeDefinition viewType, List<EClass> sourceClasses) {
        return formatClassifiers(viewType, sourceClasses, AstUtils.getImportedClassesByName(viewType));
    }

    /**
     * Formats data types with the same name as "data type X" or "data types X, Y", see {@link #formatClassifiers}.
     * A simple name is ambiguous if another data type of the packages that can be used as explicit feature type has
     * it.
     *
     * @return the formatted list with the wording and the suffix for the end of the message, see
     *     {@link #formatClassifiers}
     */
    static Pair<String, String> formatDataTypes(ViewTypeDefinition viewType, List<EDataType> dataTypes) {
        var formatted = formatClassifiers(viewType, dataTypes, AstUtils.getFeatureTypeDataTypes(viewType));
        return new Pair<>((dataTypes.size() == 1 ? "data type " : "data types ") + formatted.left(), formatted.right());
    }

    /**
     * Formats classifiers of the same kind, sorted by the written name and separated by commas. A classifier is
     * written with its simple name, unless the simple name is ambiguous: another classifier of the same kind is
     * imported with this name ({@code importedByName}) or another of the given classifiers has this name.
     *
     * @return the formatted list and the suffix for the end of the message: the namespace URIs (sorted like the
     *     names) if the written names are not unique, otherwise empty
     */
    private static Pair<String, String> formatClassifiers(
        ViewTypeDefinition viewType,
        List<? extends EClassifier> classifiers,
        Map<String, ? extends List<? extends EClassifier>> importedByName
    ) {
        var countsByName = classifiers.stream()
            .collect(Collectors.groupingBy(EClassifier::getName, Collectors.counting()));
        // written name and namespace URI, sorted by both
        var writtenNames = classifiers.stream()
            .map(classifier -> {
                var imported = importedByName.get(classifier.getName());
                var isAmbiguous = countsByName.get(classifier.getName()) > 1
                    || (imported != null && imported.stream().anyMatch(other -> other != classifier));
                return new Pair<>(
                    isAmbiguous ? AstUtils.getQualifiedNameInQuery(viewType, classifier) : classifier.getName(),
                    classifier.getEPackage().getNsURI()
                );
            })
            .sorted(Comparator.<Pair<String, String>, String>comparing(Pair::left).thenComparing(Pair::right))
            .toList();
        var names = writtenNames.stream().map(Pair::left).toList();
        var namespaceURIs = names.stream().distinct().count() < names.size()
            ? " Namespace URIs: " + writtenNames.stream().map(Pair::right).collect(Collectors.joining(", "))
            : "";
        return new Pair<>(String.join(", ", names), namespaceURIs);
    }

}
