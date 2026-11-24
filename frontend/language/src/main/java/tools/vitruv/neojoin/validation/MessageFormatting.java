package tools.vitruv.neojoin.validation;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.common.EMFUtils;
import tools.vitruv.neojoin.utils.AstUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Formats source classes, enumerations and data types for validation messages the way they are written in a query.
 * <p>
 * A classifier is written with its simple name, unless another candidate has this simple name: then with the
 * qualified name (see
 * {@link AstUtils#getQualifiedNameInQuery}), and if that is still not unique, with the qualified name followed by the
 * namespace URI in parentheses, for example
 * {@code supplier.Food (http://example.org/supplier-a)}.
 * <p>
 * The candidates are the classifiers of the same kind (classes, or data types including enumerations) that a written
 * name must be unique among: the classes of the imported packages, or the data types of the imported packages and
 * Ecore ({@link AstUtils#getFeatureTypePackagesByAlias}), each including the classifiers of the packages that these
 * reference (the types of structural features and the super types, transitively, see
 * {@link EMFUtils#getReferencedRootPackages}). Referenced packages need not be imported, so a query can use their
 * classifiers without importing them (for example through the type of a copied reference).
 */
final class MessageFormatting {

    private MessageFormatting() {}

    /**
     * The written name of a classifier, see {@link MessageFormatting}.
     *
     * @param name         simple name, or qualified name if the simple name is ambiguous
     * @param namespaceUri namespace URI if the qualified name is ambiguous too, otherwise {@code null}
     */
    record WrittenName(String name, @Nullable String namespaceUri) {

        /**
         * Returns the name followed by the namespace URI in parentheses (if any), without quotes. For lists and for
         * messages whose text is unquoted.
         */
        String plain() {
            return name + uriSuffix();
        }

        /**
         * Returns the name in single quotes, followed by the namespace URI in parentheses (if any) outside the quotes.
         */
        String quoted() {
            return "'" + name + "'" + uriSuffix();
        }

        /**
         * Returns the name and the given member in single quotes ({@code 'name::member'}), followed by the namespace
         * URI in parentheses (if any) outside the quotes.
         */
        String quotedWithMember(String member) {
            return "'" + name + "::" + member + "'" + uriSuffix();
        }

        private String uriSuffix() {
            return namespaceUri != null ? " (" + namespaceUri + ")" : "";
        }

    }

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
        List<? extends EClassifier> candidates = list.get(0) instanceof EClass
            ? getCandidateClasses(viewType)
            : getCandidateDataTypes(viewType);

        return list.stream()
            .map(classifier -> getWrittenName(viewType, classifier, candidates))
            .sorted(Comparator.comparing(WrittenName::name)
                .thenComparing(writtenName -> String.valueOf(writtenName.namespaceUri())))
            .toList();
    }

    /**
     * Returns the written name of the classifier, given the classifiers of the same kind that it can be mistaken for
     * (including the classifier itself).
     */
    private static WrittenName getWrittenName(
        ViewTypeDefinition viewType,
        EClassifier classifier,
        List<? extends EClassifier> candidates
    ) {
        var sameSimpleName = candidates.stream()
            .filter(other -> other != classifier && other.getName().equals(classifier.getName()))
            .toList();
        if (sameSimpleName.isEmpty()) {
            return new WrittenName(classifier.getName(), null);
        }

        var qualifiedName = AstUtils.getQualifiedNameInQuery(viewType, classifier);
        var isQualifiedNameAmbiguous = sameSimpleName.stream()
            .anyMatch(other -> AstUtils.getQualifiedNameInQuery(viewType, other).equals(qualifiedName));
        return new WrittenName(qualifiedName, isQualifiedNameAmbiguous ? classifier.getEPackage().getNsURI() : null);
    }

    /**
     * Returns the classes of the imported packages and of the packages that these reference.
     */
    private static List<EClass> getCandidateClasses(ViewTypeDefinition viewType) {
        return EMFUtils.getReferencedRootPackages(AstUtils.getImportedPackagesByAlias(viewType).values()).stream()
            .flatMap(EMFUtils::getAllEClassifiers)
            .filter(EClass.class::isInstance)
            .map(EClass.class::cast)
            .toList();
    }

    /**
     * Returns the data types of the packages that can be used as explicit feature type and of the packages that these
     * reference.
     */
    private static List<EDataType> getCandidateDataTypes(ViewTypeDefinition viewType) {
        var packages = AstUtils.getFeatureTypePackagesByAlias(viewType).values();
        return EMFUtils.getReferencedRootPackages(packages).stream()
            .flatMap(EMFUtils::getAllEDataTypes)
            .toList();
    }

}
