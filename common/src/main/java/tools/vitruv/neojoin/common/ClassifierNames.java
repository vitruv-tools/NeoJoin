package tools.vitruv.neojoin.common;

import static tools.vitruv.neojoin.common.Assertions.check;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.jspecify.annotations.Nullable;

/**
 * Writes classifiers (classes, enumerations and data types) for messages the way a query writes them. Messages name
 * classifiers of source meta-models, and the same simple name can belong to several of them (for example
 * {@code Food} in two meta-models); a message that only shows the simple name would not say which one is meant.
 * <p>
 * A classifier is written with its simple name, unless another candidate has this simple name: then with the
 * qualified name (see {@link #getQualifiedNameSegments}), and if that is still not unique, with the qualified name
 * followed by the namespace URI in parentheses, for example {@code supplier.Food (http://example.org/supplier-a)}.
 * <p>
 * The candidates are the classifiers of the same kind (classes, or data types including enumerations) that a written
 * name must be unique among: those of the start packages and of the packages that these reference (the types of
 * structural features and the super types, transitively, see {@link EMFUtils#getReferencedRootPackages}). A class and
 * a data type with the same name are not ambiguous, because a query never mistakes one for the other. Referenced
 * packages need not be imported, so a query can use their classifiers without importing them (for example through the
 * type of a copied reference).
 */
public final class ClassifierNames {

    private final Map<EPackage, String> aliasByRootPackage;
    private final Collection<EPackage> startPackages;

    private @Nullable List<EClassifier> classes;
    private @Nullable List<EClassifier> dataTypes;

    /**
     * The written name of a classifier, see {@link ClassifierNames}.
     *
     * @param name         simple name, or qualified name if the simple name is ambiguous
     * @param namespaceUri namespace URI if the qualified name is ambiguous too, otherwise {@code null}
     */
    public record WrittenName(String name, @Nullable String namespaceUri) {

        /**
         * Returns the name followed by the namespace URI in parentheses (if any), without quotes. For lists and for
         * messages whose text is unquoted.
         */
        public String plain() {
            return name + uriSuffix();
        }

        /**
         * Returns the name in single quotes, followed by the namespace URI in parentheses (if any) outside the
         * quotes.
         */
        public String quoted() {
            return "'" + name + "'" + uriSuffix();
        }

        /**
         * Returns the name and the given member in single quotes ({@code 'name::member'}), followed by the namespace
         * URI in parentheses (if any) outside the quotes.
         */
        public String quotedWithMember(String member) {
            return "'" + name + "::" + member + "'" + uriSuffix();
        }

        private String uriSuffix() {
            return namespaceUri != null ? " (" + namespaceUri + ")" : "";
        }

    }

    /**
     * Creates the names for the classifiers of the given start packages and of the packages that these reference.
     *
     * @param aliasByRootPackage the alias of each imported root package; the first segment of the qualified name of
     *                           a classifier in a package that is not imported is the name of its root package
     * @param startPackages      the packages whose classifiers, and those of the packages they reference, are the
     *                           candidates
     */
    public ClassifierNames(Map<EPackage, String> aliasByRootPackage, Collection<EPackage> startPackages) {
        this.aliasByRootPackage = Map.copyOf(aliasByRootPackage);
        this.startPackages = List.copyOf(startPackages);
    }

    /**
     * Returns the segments of the fully qualified name of the given classifier: the given alias of the root package,
     * the names of the sub packages and the name of the classifier.
     *
     * @param classifier classifier to get the fully qualified name segments for
     * @param rootAlias  alias to use as the first segment (e.g. the root package alias)
     * @return segments of the fully qualified name of the classifier
     */
    public static List<String> getQualifiedNameSegments(EClassifier classifier, String rootAlias) {
        var segments = Stream.concat(
            EMFUtils.getFullyQualifiedNameAsStream(classifier.getEPackage()),
            Stream.of(classifier.getName())
        ).collect(Collectors.toCollection(ArrayList::new));
        check(segments.size() > 1); // at least package name + class name
        segments.set(0, rootAlias);
        return List.copyOf(segments);
    }

    /**
     * Returns the written name of the given classifier, see {@link ClassifierNames}. The candidates are computed
     * once per kind of classifier.
     */
    public WrittenName write(EClassifier classifier) {
        var sameSimpleName = getCandidates(classifier).stream()
            .filter(other -> other != classifier && other.getName().equals(classifier.getName()))
            .toList();
        if (sameSimpleName.isEmpty()) {
            return new WrittenName(classifier.getName(), null);
        }

        var qualifiedName = getQualifiedName(classifier);
        var isQualifiedNameAmbiguous = sameSimpleName.stream()
            .anyMatch(other -> getQualifiedName(other).equals(qualifiedName));
        return new WrittenName(qualifiedName, isQualifiedNameAmbiguous ? classifier.getEPackage().getNsURI() : null);
    }

    private String getQualifiedName(EClassifier classifier) {
        var rootPackage = EMFUtils.getRootPackage(classifier.getEPackage());
        var alias = aliasByRootPackage.getOrDefault(rootPackage, rootPackage.getName());
        return String.join(".", getQualifiedNameSegments(classifier, alias));
    }

    /**
     * Returns the candidates of the same kind as the given one (class or data type).
     */
    private List<EClassifier> getCandidates(EClassifier classifier) {
        if (classifier instanceof EClass) {
            if (classes == null) {
                classes = collectCandidates(EClass.class::isInstance);
            }
            return classes;
        }

        if (dataTypes == null) {
            dataTypes = collectCandidates(EDataType.class::isInstance);
        }
        return dataTypes;
    }

    private List<EClassifier> collectCandidates(Predicate<EClassifier> isKind) {
        return EMFUtils.getReferencedRootPackages(startPackages).stream()
            .flatMap(EMFUtils::getAllEClassifiers)
            .filter(isKind)
            .toList();
    }

}
