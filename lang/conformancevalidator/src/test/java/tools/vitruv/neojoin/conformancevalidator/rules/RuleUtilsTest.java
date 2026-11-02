package tools.vitruv.neojoin.conformancevalidator.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the conditions and message helpers of {@link RuleUtils}.
 * <p>
 * The fixture is a small metamodel built in memory. It contains a classifier that a mandatory
 * feature requires, one that nothing requires, a subpackage whose classifier is required from
 * outside and a subpackage whose classifier is not, so that both outcomes of every condition
 * can be checked.
 */
class RuleUtilsTest {

    private static final EcoreFactory FACTORY = EcoreFactory.eINSTANCE;

    private EPackage shop;
    private EPackage internal;
    private EPackage extras;

    private EClass order;
    private EClass customer;
    private EClass premiumCustomer;
    private EClass note;
    private EClass audit;
    private EClass tag;
    private EEnum status;

    private EAttribute orderId;
    private EReference orderCustomer;
    private EReference orderAudits;

    @BeforeEach
    void buildFixture() {
        shop = pack("shop");
        internal = pack("internal");
        extras = pack("extras");
        shop.getESubpackages().add(internal);
        shop.getESubpackages().add(extras);

        customer = clazz("Customer");
        customer.getEStructuralFeatures().add(attribute("name", EcorePackage.Literals.ESTRING, 0, 1));

        premiumCustomer = clazz("PremiumCustomer");
        premiumCustomer.getESuperTypes().add(customer);

        note = clazz("Note");
        note.getEStructuralFeatures().add(attribute("text", EcorePackage.Literals.ESTRING, 0, 1));

        audit = clazz("Audit");
        audit.getEStructuralFeatures().add(attribute("entry", EcorePackage.Literals.ESTRING, 0, 1));

        tag = clazz("Tag");
        tag.getEStructuralFeatures().add(attribute("label", EcorePackage.Literals.ESTRING, 0, 1));

        status = FACTORY.createEEnum();
        status.setName("Status");
        status.getELiterals().add(literal("NEW", 0));
        status.getELiterals().add(literal("DONE", 1));

        orderId = attribute("id", EcorePackage.Literals.ESTRING, 0, 1);
        orderCustomer = reference("customer", customer, 1, 1);
        orderAudits = reference("audits", audit, 1, -1);

        order = clazz("Order");
        order.getEStructuralFeatures().add(orderId);
        order.getEStructuralFeatures().add(orderCustomer);
        order.getEStructuralFeatures().add(orderAudits);
        order.getEStructuralFeatures().add(reference("notes", note, 0, -1));

        shop.getEClassifiers().add(order);
        shop.getEClassifiers().add(customer);
        shop.getEClassifiers().add(premiumCustomer);
        shop.getEClassifiers().add(note);
        shop.getEClassifiers().add(status);
        internal.getEClassifiers().add(audit);
        extras.getEClassifiers().add(tag);
    }

    // ----- isRequired -------------------------------------------------------

    @Test
    void classifierWithMandatoryFeatureIsRequired() {
        assertTrue(RuleUtils.isRequired(shop, customer));
    }

    @Test
    void classifierWithOnlyOptionalFeatureIsNotRequired() {
        assertFalse(RuleUtils.isRequired(shop, note));
    }

    @Test
    void classifierNobodyPointsAtIsNotRequired() {
        assertFalse(RuleUtils.isRequired(shop, tag));
    }

    /**
     * The search has to start at the root package, so that a classifier required from another
     * part of the metamodel is found even when the call starts in a subpackage.
     */
    @Test
    void requirementIsFoundFromASubpackage() {
        assertTrue(RuleUtils.isRequired(internal, customer));
    }

    // ----- containsRequiredClassifier ---------------------------------------

    @Test
    void subpackageWithARequiredClassifierIsDetected() {
        assertTrue(RuleUtils.containsRequiredClassifier(internal));
    }

    @Test
    void subpackageWithoutARequiredClassifierIsNotDetected() {
        assertFalse(RuleUtils.containsRequiredClassifier(extras));
    }

    // ----- rootOf -----------------------------------------------------------

    @Test
    void rootOfASubpackageIsTheOutermostPackage() {
        assertSame(shop, RuleUtils.rootOf(internal));
    }

    @Test
    void rootOfARootPackageIsItself() {
        assertSame(shop, RuleUtils.rootOf(shop));
    }

    // ----- qualifiedName ----------------------------------------------------

    @Test
    void featureIsQualifiedWithItsDeclaringClass() {
        assertEquals("Order.id", RuleUtils.qualifiedName(orderId));
    }

    @Test
    void elementWithoutADeclaringClassKeepsItsPlainName() {
        assertEquals("loose", RuleUtils.qualifiedName(
            attribute("loose", EcorePackage.Literals.ESTRING, 0, 1)));
    }

    @Test
    void literalIsQualifiedWithItsEnumeration() {
        assertEquals("Status.DONE", RuleUtils.qualifiedName(status.getELiterals().get(1)));
    }

    @Test
    void literalWithoutAnEnumerationKeepsItsPlainName() {
        assertEquals("LOOSE", RuleUtils.qualifiedName(literal("LOOSE", 0)));
    }

    // ----- bound ------------------------------------------------------------

    @Test
    void unboundedUpperBoundIsRenderedAsAStar() {
        assertEquals("*", RuleUtils.bound(-1));
    }

    @Test
    void ordinaryBoundIsRenderedAsItsNumber() {
        assertEquals("0", RuleUtils.bound(0));
        assertEquals("3", RuleUtils.bound(3));
    }

    // ----- requiringFeatureName and requiredFromOutside ----------------------

    @Test
    void theRequiringFeatureIsNamed() {
        assertEquals("Order.customer", RuleUtils.requiringFeatureName(shop, customer));
    }

    @Test
    void aGenericPhraseIsUsedWhenNoFeatureRequiresTheClassifier() {
        assertEquals("a feature of the reference", RuleUtils.requiringFeatureName(shop, note));
    }

    @Test
    void theFeatureRequiringASubpackageIsNamedTogetherWithTheClassifier() {
        String phrase = RuleUtils.requiredFromOutside(internal);
        assertTrue(phrase.contains("Order.audits"), phrase);
        assertTrue(phrase.contains("Audit"), phrase);
    }

    @Test
    void aGenericPhraseIsUsedWhenNothingRequiresTheSubpackage() {
        assertEquals("a feature of the reference requires one of its classifiers",
            RuleUtils.requiredFromOutside(extras));
    }

    // ----- isCompatibleType -------------------------------------------------

    @Test
    void theSameClassifierIsCompatible() {
        assertTrue(RuleUtils.isCompatibleType(customer, customer));
    }

    @Test
    void aSubtypeIsCompatibleWithItsSupertype() {
        assertTrue(RuleUtils.isCompatibleType(premiumCustomer, customer));
    }

    @Test
    void aSupertypeIsNotCompatibleWithItsSubtype() {
        assertFalse(RuleUtils.isCompatibleType(customer, premiumCustomer));
    }

    @Test
    void unrelatedDataTypesAreNotCompatible() {
        assertFalse(RuleUtils.isCompatibleType(
            EcorePackage.Literals.ESTRING, EcorePackage.Literals.EINT));
    }

    // ----- higherUpperBound -------------------------------------------------

    @Test
    void anUnboundedViewTypeAllowsMoreThanABoundedReference() {
        assertTrue(RuleUtils.higherUpperBound(-1, 5));
    }

    @Test
    void anUnboundedReferenceAllowsEverythingTheViewTypeCan() {
        assertFalse(RuleUtils.higherUpperBound(5, -1));
        assertFalse(RuleUtils.higherUpperBound(-1, -1));
    }

    @Test
    void aHigherBoundInTheViewTypeAllowsMore() {
        assertTrue(RuleUtils.higherUpperBound(7, 5));
    }

    @Test
    void anEqualOrLowerBoundInTheViewTypeAllowsNoMore() {
        assertFalse(RuleUtils.higherUpperBound(5, 5));
        assertFalse(RuleUtils.higherUpperBound(3, 5));
    }

    // ----- existsOnBothSides ------------------------------------------------

    /**
     * A classifier that only the left side declares is itself contained in a package that both
     * sides have, so the difference for the classifier passes the check, while the difference
     * for a feature inside that classifier does not. This is what keeps the contents of an added
     * classifier from being reported once per feature.
     */
    @Test
    void onlyTheDifferenceOfTheAddedClassifierExistsOnBothSides() {
        EPackage left = pack("compare");
        EPackage right = pack("compare");

        EClass added = clazz("Added");
        added.getEStructuralFeatures().add(attribute("field", EcorePackage.Literals.ESTRING, 0, 1));
        left.getEClassifiers().add(added);

        Comparison comparison = EMFCompare.builder().build()
            .compare(new DefaultComparisonScope(left, right, null));
        List<Diff> diffs = comparison.getDifferences();

        Diff classifierDiff = diffOn(diffs, "eClassifiers");
        Diff featureDiff = diffOn(diffs, "eStructuralFeatures");

        assertTrue(RuleUtils.existsOnBothSides(classifierDiff));
        assertFalse(RuleUtils.existsOnBothSides(featureDiff));
    }

    // ----- fixture helpers --------------------------------------------------

    private static Diff diffOn(List<Diff> diffs, String referenceName) {
        return diffs.stream()
            .filter(ReferenceChange.class::isInstance)
            .map(ReferenceChange.class::cast)
            .filter(change -> change.getReference().getName().equals(referenceName))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no difference on " + referenceName));
    }

    private static EPackage pack(String name) {
        EPackage result = FACTORY.createEPackage();
        result.setName(name);
        result.setNsURI("http://example.org/" + name);
        result.setNsPrefix(name);
        return result;
    }

    private static EClass clazz(String name) {
        EClass result = FACTORY.createEClass();
        result.setName(name);
        return result;
    }

    private static EAttribute attribute(String name, EClassifier type, int lower, int upper) {
        EAttribute result = FACTORY.createEAttribute();
        result.setName(name);
        result.setEType(type);
        result.setLowerBound(lower);
        result.setUpperBound(upper);
        return result;
    }

    private static EReference reference(String name, EClassifier type, int lower, int upper) {
        EReference result = FACTORY.createEReference();
        result.setName(name);
        result.setEType(type);
        result.setLowerBound(lower);
        result.setUpperBound(upper);
        return result;
    }

    private static EEnumLiteral literal(String name, int value) {
        EEnumLiteral result = FACTORY.createEEnumLiteral();
        result.setName(name);
        result.setValue(value);
        return result;
    }
}
