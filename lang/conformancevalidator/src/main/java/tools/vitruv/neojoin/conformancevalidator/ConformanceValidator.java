package tools.vitruv.neojoin.conformancevalidator;

import org.eclipse.emf.compare.*;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EPackage;

import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.Strictness;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable.*;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable.*;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.*;

import java.util.ArrayList;
import java.util.List;

public class ConformanceValidator {

    private static final List<ConformanceRule> ALL_RULES = List.of(
        new FeatureAddedRule(),              // R2
        new FeatureDeletedRule(),            // R3
        new SafeFeatureDeletedRule(),        // R4
        new LowerBoundChangedRule(),         // R5
        new SafeLowerBoundChangedRule(),     // R6
        new UpperBoundChangedRule(),         // R7
        new SafeUpperBoundChangedRule(),     // R8
        new TypeChangedRule(),               // R9
        new SafeTypeChangedRule(),           // R10
        new ClassDeletedRule(),              // R11
        new SafeClassDeletedRule(),          // R12
        new SuperTypeDeletedRule(),          // R13
        new SuperTypeAddedRule(),            // R14
        new SafeSuperTypeDeletedRule(),      // R15
        new EnumDeletedRule(),               // R16
        new SafeEnumDeletedRule(),           // R17
        new EnumLiteralAddedRule(),          // R18
        new EnumLiteralDeletedRule(),        // R19
        new EnumAddedRule()                  // R28
    );

    /**
     * Phase II: evaluates all diffs by delegating to the rule classes.
     * Left = view type, Right = reference.
     *
     * @param diffs the diffs produced by Phase I
     * @return list of violations
     */
    private List<Violation> evaluateDiffs(List<Diff> diffs, Strictness strictness) {
        List<Violation> violations = new ArrayList<>();
         List<ConformanceRule> activeRules = ALL_RULES.stream()
            .filter(rule -> strictness.getAllowedCategories().contains(rule.getCategory()))
            .toList();
        for (Diff diff : diffs) {
            for (ConformanceRule rule : activeRules) {
                rule.check(diff).ifPresent(violations::add);
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
     * @return list of violations
     */
    public List<Violation> validatePackages(EPackage viewType, EPackage reference, Strictness strictness) {
        List<Diff> diffs = compare(viewType, reference);
        return evaluateDiffs(diffs, strictness);
    }

    /**
     * Phase I: Extracts structural differences between view type and reference
     * using EMF Compare. Left = view type, right = reference.
     *
     * @param viewType  the generated view type EPackage (left)
     * @param reference the reference metamodel EPackage (right)
     * @return list of detected differences
     */
    private List<Diff> compare(EPackage viewType, EPackage reference) {
        IComparisonScope scope = new DefaultComparisonScope(viewType, reference, null);
        Comparison comparison = EMFCompare.builder().build().compare(scope);
        return comparison.getDifferences();
    }
}
