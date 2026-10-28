package tools.vitruv.neojoin.conformancevalidator.rules;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.util.EcoreUtil;

import java.util.stream.Stream;

/**
 * Conditions and message helpers that more than one conformance rule needs, so that each of
 * them is defined once.
 */
public final class RuleUtils {

    private RuleUtils() {}

    /**
     *
     * Checks whether any class in the pack declares a feature that demands at least one
     * value of the given classifier.
     *
     * @param pack the package to search in
     * @param classifier the classifier to look for
     * @return true if some feature has this classifier as its type and a lowerbound >= 1
     */
    public static boolean isRequired(EPackage pack, EClassifier classifier) {
        return allClasses(rootOf(pack))
            .flatMap(c -> c.getEAllStructuralFeatures().stream())
            .anyMatch(f -> f.getEType() == classifier && f.getLowerBound() >= 1);
    }

    /**
     * Checks whether the given package contains a classifier that a feature outside the
     * package demands.
     *
     * @param pack the package to search in
     * @return true if a feature outside the package requires one of its classifiers
     */
    public static boolean containsRequiredClassifier(EPackage pack) {
        return allClasses(rootOf(pack))
            .filter(owner -> !EcoreUtil.isAncestor(pack, owner))
            .flatMap(owner -> owner.getEAllStructuralFeatures().stream())
            .anyMatch(f -> f.getLowerBound() >= 1
                && f.getEType() != null
                && EcoreUtil.isAncestor(pack, f.getEType()));
    }

    /**
     * Checks whether the element that contains the changed value exists in the view type and
     * in the reference.
     *
     * @param diff the difference
     * @return true if the containing element exists on both sides
     */
    public static boolean existsOnBothSides(Diff diff) {
        Match match = diff.getMatch();
        return match != null && match.getLeft() != null && match.getRight() != null;
    }

    /**
     * Gets the super package until it reaches the root.
     *
     * @param pack the package to start from
     * @return the outermost package
     */
    public static EPackage rootOf(EPackage pack) {
        EPackage current = pack;
        while (current.getESuperPackage() != null) {
            current = current.getESuperPackage();
        }
        return current;
    }

    /**
     * Collects the classes of a package and of all its subpackages.
     *
     * @param pack the package to start from
     * @return every class in the given package
     */
    private static Stream<EClass> allClasses(EPackage pack) {
        return Stream.concat(
            pack.getEClassifiers().stream().filter(EClass.class::isInstance).map(EClass.class::cast),
            pack.getESubpackages().stream().flatMap(RuleUtils::allClasses)
        );
    }

    /**
     * Gets the name of the features and its class, elements that are not features keep their name.
     *
     * @param element the element to name
     * @return the qualified name
     */
    public static String qualifiedName(ETypedElement element) {
        if (element instanceof EStructuralFeature feature && feature.getEContainingClass() != null) {
            return feature.getEContainingClass().getName() + "." + feature.getName();
        }
        return element.getName();
    }

    /**
     * Gets the name of an enum literal with its enum class name.
     *
     * @param literal the literal to name
     * @return the qualified name
     */
    public static String qualifiedName(EEnumLiteral literal) {
        return literal.getEEnum() == null
            ? literal.getName()
            : literal.getEEnum().getName() + "." + literal.getName();
    }

    /**
     * Renders a bound for a message.
     *
     * @param value the bound as Ecore stores it
     * @return the bound as it appears in a message
     */
    public static String bound(int value) {
        return value == -1 ? "*" : Integer.toString(value);
    }

    /**
     * Names a feature that demands at least one value of the given classifier.
     *
     * @param pack the package to search in
     * @param classifier the classifier that is required
     * @return the qualified name of the first feature, or a generic phrase if none is found
     */
    public static String requiringFeatureName(EPackage pack, EClassifier classifier) {
        return allClasses(rootOf(pack))
            .flatMap(c -> c.getEAllStructuralFeatures().stream())
            .filter(f -> f.getEType() == classifier && f.getLowerBound() >= 1)
            .findFirst()
            .map(RuleUtils::qualifiedName)
            .orElse("a feature of the reference");
    }

    /**
     * Names a feature outside the given package that demands at least one value of one of its
     * classifiers, together with that classifier.
     *
     * @param pack the package whose classifiers are required
     * @return a phrase of the feature and its required classifier, or a generic
     * phrase if no such feature is found
     */
    public static String requiredFromOutside(EPackage pack) {
        return allClasses(rootOf(pack))
            .filter(owner -> !EcoreUtil.isAncestor(pack, owner))
            .flatMap(owner -> owner.getEAllStructuralFeatures().stream())
            .filter(f -> f.getLowerBound() >= 1
                && f.getEType() != null
                && EcoreUtil.isAncestor(pack, f.getEType()))
            .findFirst()
            .map(f -> "'" + qualifiedName(f) + "' requires its classifier '" + f.getEType().getName() + "'")
            .orElse("a feature of the reference requires one of its classifiers");
    }

    /**
     * Checks whether a value of the view type classifier can be used where the reference
     * classifier is expected.
     *
     * @param viewTypeClassifier the classifier the view type declares
     * @param referenceClassifier the classifier the reference declares
     * @return true if every value of the view type classifier is accepted by the reference
     */
    public static boolean isCompatibleType(EClassifier viewTypeClassifier, EClassifier referenceClassifier) {
        if (viewTypeClassifier.getName().equals(referenceClassifier.getName())) {
            return true;
        }
        if (viewTypeClassifier instanceof EClass viewClass && referenceClassifier instanceof EClass refClass) {
            return viewClass.getEAllSuperTypes().stream()
                .anyMatch(superType -> superType.getName().equals(refClass.getName()));
        }
        return false;
    }

    /**
     * Checks whether the view type allows more values for a feature than the reference does.
     *
     * @param viewUpperBound the upperbound the view type declares
     * @param referenceUpperBound the upperbound the reference declares
     * @return true if the view type allows a number of values the reference rejects
     */
    public static boolean higherUpperBound(int viewUpperBound, int referenceUpperBound) {
        return referenceUpperBound != -1
            && (viewUpperBound == -1 || viewUpperBound > referenceUpperBound);
    }
}
