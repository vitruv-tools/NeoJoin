package tools.vitruv.neojoin.conformancevalidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import tools.vitruv.neojoin.conformancevalidator.rules.Strictness;

/**
 * Integration test of the whole rule catalogue. Each case-study pair in
 * {@code src/test/resources/case-studies} is a hand-written view type together with a reference
 * meta-model, built to trigger a known set of rules (documented in the header of the
 * {@code .ecore} files). The pairs are validated through {@link ConformanceValidator}, so both
 * phases run: the comparison with EMF Compare and the evaluation of the rules.
 */
class ConformanceValidatorIntegrationTest {

    /**
     * One case-study pair.
     *
     * @param name the pair directory, e.g. {@code Pair_A}
     * @param viewType the view type file in that directory
     * @param reference the reference file in that directory
     * @param expectedRules the rules the pair triggers under {@link Strictness#EXACT_MATCH}
     * @param conformingUnderSubtype whether the pair has no breaking violation
     */
    private record Pair(String name, String viewType, String reference, Set<String> expectedRules,
                        boolean conformingUnderSubtype) {}

    private static final List<Pair> PAIRS = List.of(
        new Pair("Pair_A", "actor-rating-viewtype.ecore", "actor-rating-reference.ecore",
            Set.of("Rule 3", "Rule 11", "Rule 16", "Rule 29"), false),
        new Pair("Pair_B", "movies-viewtype.ecore", "movies-reference.ecore",
            Set.of("Rule 2", "Rule 14", "Rule 26", "Rule 27", "Rule 28"), true),
        new Pair("Pair_C", "library-viewtype.ecore", "library-reference.ecore",
            Set.of("Rule 4", "Rule 6", "Rule 8", "Rule 10", "Rule 21", "Rule 24"), true),
        new Pair("Pair_D", "reviews-viewtype.ecore", "reviews-reference.ecore",
            Set.of("Rule 2", "Rule 5", "Rule 7", "Rule 20", "Rule 22"), false),
        new Pair("Pair_E", "restaurant-viewtype.ecore", "restaurant-reference.ecore",
            Set.of("Rule 4", "Rule 12", "Rule 15", "Rule 17", "Rule 19", "Rule 30"), true),
        new Pair("Pair_F", "smarthome-viewtype.ecore", "smarthome-reference.ecore",
            Set.of("Rule 9", "Rule 13", "Rule 18", "Rule 23", "Rule 25"), false)
    );

    private final ConformanceValidator validator = new ConformanceValidator();

    @TestFactory
    Stream<DynamicTest> exactMatchReportsExactlyTheExpectedRules() {
        return PAIRS.stream().map(pair -> dynamicTest(pair.name(), () -> {
            assertEquals(pair.expectedRules(), firedRules(pair, Strictness.EXACT_MATCH));
        }));
    }

    @TestFactory
    Stream<DynamicTest> subtypeReportsOnlyBreakingViolations() {
        return PAIRS.stream().map(pair -> dynamicTest(pair.name(), () -> {
            List<Violation> violations = validate(pair, Strictness.SUBTYPE);

            assertEquals(pair.conformingUnderSubtype(), violations.isEmpty(), violations.toString());
            assertTrue(violations.stream().allMatch(v -> v.category() != Category.NON_BREAKING),
                violations.toString());
        }));
    }

    @TestFactory
    Stream<DynamicTest> subtypeReportsTheBreakingSubsetOfTheExactMatchViolations() {
        return PAIRS.stream().map(pair -> dynamicTest(pair.name(), () -> {
            Set<String> breaking = validate(pair, Strictness.EXACT_MATCH).stream()
                .filter(v -> v.category() != Category.NON_BREAKING)
                .map(Violation::ruleId)
                .collect(Collectors.toCollection(TreeSet::new));

            assertEquals(breaking, firedRules(pair, Strictness.SUBTYPE));
        }));
    }

    @TestFactory
    Stream<DynamicTest> everyViolationCarriesTheCategoryOfItsRule() {
        return PAIRS.stream().map(pair -> dynamicTest(pair.name(), () -> {
            for (Violation violation : validate(pair, Strictness.EXACT_MATCH)) {
                assertTrue(!violation.message().isBlank(), violation.ruleId());
                assertTrue(violation.diff() != null, violation.ruleId());
            }
        }));
    }

    @Test
    void theCaseStudiesTriggerEveryRuleOfTheCatalogue() {
        Set<String> catalogue = IntStream.rangeClosed(2, 30)
            .mapToObj(i -> "Rule " + i)
            .collect(Collectors.toCollection(TreeSet::new));
        Set<String> expected = PAIRS.stream()
            .flatMap(pair -> pair.expectedRules().stream())
            .collect(Collectors.toCollection(TreeSet::new));
        Set<String> fired = new TreeSet<>();
        for (Pair pair : PAIRS) {
            fired.addAll(firedRules(pair, Strictness.EXACT_MATCH));
        }

        assertEquals(catalogue, expected, "the expectations must cover Rule 2 to Rule 30");
        assertEquals(catalogue, fired);
    }

    private Set<String> firedRules(Pair pair, Strictness strictness) {
        return validate(pair, strictness).stream()
            .map(Violation::ruleId)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private List<Violation> validate(Pair pair, Strictness strictness) {
        EPackage viewType = load(pair.name() + "/" + pair.viewType());
        EPackage reference = load(pair.name() + "/" + pair.reference());
        return validator.validatePackages(viewType, reference, strictness);
    }

    private static EPackage load(String resource) {
        try {
            var url = ConformanceValidatorIntegrationTest.class.getResource("/case-studies/" + resource);
            assertTrue(url != null, "missing test resource " + resource);
            ResourceSet resourceSet = new ResourceSetImpl();
            resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put("ecore", new EcoreResourceFactoryImpl());
            var uri = URI.createFileURI(Path.of(url.toURI()).toString());
            return (EPackage) resourceSet.getResource(uri, true).getContents().get(0);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
