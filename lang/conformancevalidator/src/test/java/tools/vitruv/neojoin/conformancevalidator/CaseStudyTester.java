package tools.vitruv.neojoin.conformancevalidator;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;

import tools.vitruv.neojoin.conformancevalidator.rules.Strictness;

import java.util.List;

/**
 * Runs the conformance validator against the case-study pairs and prints, for each
 * pair, the differences reported by EMF Compare and the violations reported by the
 * rules. Used to produce the numbers for the evaluation chapter.
 * <p>
 * Not part of the validator itself: it lives in the test sources and is therefore
 * never packaged into the module artifact.
 * <p>
 * Violations are collected with {@link Strictness#EXACT_MATCH} so that non-breaking
 * rules appear as well; the verdict under {@link Strictness#SUBTYPE} is reported
 * separately, because that is the strictness a conformance check uses by default.
 */
public class CaseStudyTester {

    private static final String BASE =
        "C:/Users/fbach/OneDrive/Dokumente/Bachelorarbeit/Case-Study-Ecore/";

    /**
     * One case-study pair: name, view type file, reference file and the rules the
     * pair is expected to trigger (see the comments in the .ecore files).
     * <p>
     * Rule 0 and Rule 1 never appear here: they state that a difference is conforming by
     * definition, so there is no rule class for them and they can only be observed in
     * the list of differences.
     */
    private record Pair(String name, String viewType, String reference, String[] expectedRules) {}

    private static final List<Pair> PAIRS = List.of(
        new Pair("A  missing required elements",
            "Pair_A/actor-rating-viewtype.ecore", "Pair_A/actor-rating-reference.ecore",
            new String[] {"Rule 3", "Rule 11", "Rule 16", "Rule 29"}),
        new Pair("B  additional structure",
            "Pair_B/movies-viewtype.ecore", "Pair_B/movies-reference.ecore",
            new String[] {"Rule 2", "Rule 14", "Rule 26", "Rule 27", "Rule 28"}),
        new Pair("C  stricter constraints",
            "Pair_C/library-viewtype.ecore", "Pair_C/library-reference.ecore",
            new String[] {"Rule 4", "Rule 6", "Rule 8", "Rule 10", "Rule 21", "Rule 24"}),
        new Pair("D  weakened constraints",
            "Pair_D/reviews-viewtype.ecore", "Pair_D/reviews-reference.ecore",
            new String[] {"Rule 2", "Rule 5", "Rule 7", "Rule 20", "Rule 22"}),
        new Pair("E  harmless omissions",
            "Pair_E/restaurant-viewtype.ecore", "Pair_E/restaurant-reference.ecore",
            new String[] {"Rule 4", "Rule 12", "Rule 15", "Rule 17", "Rule 19", "Rule 30"}),
        new Pair("F  incompatible structure",
            "Pair_F/smarthome-viewtype.ecore", "Pair_F/smarthome-reference.ecore",
            new String[] {"Rule 9", "Rule 13", "Rule 18", "Rule 23", "Rule 25"})
    );

    /** Discarded runs that let the JIT compiler warm up before measuring. */
    private static final int WARMUP_RUNS = 10;

    /** Measured runs per pair; the median of these is reported. */
    private static final int MEASURED_RUNS = 50;

    public static void main(String[] args) {
        var validator = new ConformanceValidator();
        var timings = new java.util.LinkedHashMap<String, Double>();

        for (Pair pair : PAIRS) {
            EPackage viewType = load(BASE + pair.viewType());
            EPackage reference = load(BASE + pair.reference());

            System.out.println("=".repeat(78));
            System.out.println("Pair " + pair.name());
            System.out.println("=".repeat(78));

            System.out.println("\n-- differences reported by EMF Compare --");
            List<Diff> diffs = compare(viewType, reference);
            if (diffs.isEmpty()) {
                System.out.println("(none)");
            }
            for (Diff diff : diffs) {
                System.out.printf("%-8s %s%n", diff.getKind(), describe(diff));
            }

            System.out.println("\n-- violations (EXACT_MATCH, all categories) --");
            List<Violation> all = validator.validatePackages(viewType, reference, Strictness.EXACT_MATCH);
            if (all.isEmpty()) {
                System.out.println("(none)");
            }
            for (Violation violation : all) {
                System.out.printf("%-24s %s%n", violation.category(), violation);
            }

            List<Violation> breaking = validator.validatePackages(viewType, reference, Strictness.SUBTYPE);
            System.out.println("\n-- verdict --");
            System.out.println("SUBTYPE:     " + (breaking.isEmpty() ? "CONFORMING" : "NOT CONFORMING")
                + " (" + breaking.size() + " violations)");
            System.out.println("EXACT_MATCH: " + (all.isEmpty() ? "CONFORMING" : "NOT CONFORMING")
                + " (" + all.size() + " violations)");

            System.out.println("\n-- expected rules --");
            List<String> fired = all.stream().map(Violation::ruleId).distinct().sorted().toList();
            List<String> expected = List.of(pair.expectedRules());
            System.out.println("fired:      " + fired);
            System.out.println("missing:    " + expected.stream().filter(r -> !fired.contains(r)).toList());
            System.out.println("unexpected: " + fired.stream().filter(r -> !expected.contains(r)).toList());

            double median = measure(validator, viewType, reference);
            timings.put(pair.name(), median);
            System.out.println("\n-- execution time --");
            System.out.printf("median of %d runs: %.2f ms%n", MEASURED_RUNS, median);
            System.out.println();
        }

        System.out.println("=".repeat(78));
        System.out.println("Execution time per pair (median of " + MEASURED_RUNS + " runs, ms)");
        System.out.println("=".repeat(78));
        timings.forEach((name, ms) -> System.out.printf("%-34s %8.2f%n", name, ms));
        System.out.printf("%-34s %8.2f%n", "slowest pair",
            timings.values().stream().mapToDouble(Double::doubleValue).max().orElse(0));
    }

    /**
     * Runs the complete conformance check repeatedly and returns the median duration in
     * milliseconds. Both phases are included, so the number covers the comparison by EMF
     * Compare as well as the evaluation of the rules. Generating the view type from the
     * query is not part of it, because that step belongs to NeoJoin and happens whether
     * or not a reference metamodel is given.
     */
    private static double measure(ConformanceValidator validator, EPackage viewType, EPackage reference) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            validator.validatePackages(viewType, reference, Strictness.EXACT_MATCH);
        }
        double[] samples = new double[MEASURED_RUNS];
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long start = System.nanoTime();
            validator.validatePackages(viewType, reference, Strictness.EXACT_MATCH);
            samples[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        java.util.Arrays.sort(samples);
        int middle = samples.length / 2;
        return samples.length % 2 == 0
            ? (samples[middle - 1] + samples[middle]) / 2
            : samples[middle];
    }

    private static EPackage load(String path) {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
            .put("ecore", new EcoreResourceFactoryImpl());
        Resource resource = resourceSet.getResource(URI.createFileURI(path), true);
        return (EPackage) resource.getContents().get(0);
    }

    /**
     * The same camparison of the validator of Phase I, so the diffs can be printed
     * alongside the violations.
     */
    private static List<Diff> compare(EPackage viewType, EPackage reference) {
        IComparisonScope scope = new DefaultComparisonScope(viewType, reference, null);
        Comparison comparison = EMFCompare.builder().build().compare(scope);
        return comparison.getDifferences();
    }

    /**
     * Shortens the default toString of a diff to the part that is readable in a log.
     */
    private static String describe(Diff diff) {
        String text = diff.toString().replaceAll("\\s+", " ");
        return text.length() > 160 ? text.substring(0, 160) + " ..." : text;
    }
}
