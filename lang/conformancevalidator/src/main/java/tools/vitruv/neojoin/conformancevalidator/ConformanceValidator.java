package tools.vitruv.neojoin.conformancevalidator;

import org.eclipse.emf.compare.*;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EPackage;

import tools.vitruv.neojoin.conformancevalidator.rules.CompatibleTypeChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.FeatureAddedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.LowerBoundChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.OptionalFeatureDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RequiredClassDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RequiredFeatureDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.SafeLowerBoundChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.SafeSuperTypeDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.SafeUpperBoundChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.SuperTypeAddedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.SuperTypeDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.TypeChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.UnrequiredClassDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.UpperBoundChangedRule;

import java.util.ArrayList;
import java.util.List;

public class ConformanceValidator {

    private static final List<ConformanceRule> ALL_RULES = List.of(
        new FeatureAddedRule(),              // R2
        new RequiredFeatureDeletedRule(),    // R3
        new OptionalFeatureDeletedRule(),    // R4
        new LowerBoundChangedRule(),         // R5
        new SafeLowerBoundChangedRule(),     // R6
        new UpperBoundChangedRule(),         // R7
        new SafeUpperBoundChangedRule(),     // R8
        new TypeChangedRule(),               // R9
        new CompatibleTypeChangedRule(),     // R10
        new RequiredClassDeletedRule(),      // R11
        new UnrequiredClassDeletedRule(),    // R12
        new SuperTypeDeletedRule(),          // R13
        new SuperTypeAddedRule(),            // R14
        new SafeSuperTypeDeletedRule()       // R15
    );

    /**
     * Phase II: evaluates all diffs by delegating to the operator classes.
     * Left = view type, Right = reference.
     * @param diffs
     * @return list of violation messages; empty means conforming.
     */
    public static List<Violation> evaluateDiffs(List<Diff> diffs) {
        List<Violation> violations = new ArrayList<>();
        for (Diff diff : diffs) {
            for (ConformanceRule rule : ALL_RULES) {
                violations.addAll(rule.check(diff));
            }
        }
        return violations;
    }

    /**
     * Validates whether the given view type conforms to the reference metamodel.
     * Includes Phase I and Phase II.
     *
     * @param viewType  the generated view type EPackage (left)
     * @param reference the reference metamodel EPackage (right)
     * @return list of violation messages; empty means conforming.
     */
    public static List<Violation> validatePackages(EPackage viewType, EPackage reference) {
        List<Diff> diffs = compare(viewType, reference);
        return evaluateDiffs(diffs);
    }

    /**
     * Phase I: Extracts structural differences between view type and reference
     * using EMF Compare. Left = view type (new), right = reference (old).
     *
     * @param viewType  the generated view type EPackage (left)
     * @param reference the reference metamodel EPackage (right)
     * @return list of detected differences
     */
    private static List<Diff> compare(EPackage viewType, EPackage reference) {
        IComparisonScope scope = new DefaultComparisonScope(viewType, reference, null);
        Comparison comparison = EMFCompare.builder().build().compare(scope);
        return comparison.getDifferences();
    }
}