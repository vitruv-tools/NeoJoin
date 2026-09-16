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

/**
 * Entry point of the conformance check.
 * The validator compares a generated view type against a reference metamodel in two phases.
 * Phase I collects the structural differences between the two metamodels with EMF Compare,
 * Phase II evaluates each of those differences against the rules.
 */
public class ConformanceValidator {

    private static final List<ConformanceRule> ALL_RULES = List.of(
        new FeatureAddedRule(),
        new FeatureDeletedRule(),
        new SafeFeatureDeletedRule(),
        new LowerBoundChangedRule(),
        new SafeLowerBoundChangedRule(),
        new UpperBoundChangedRule(),
        new SafeUpperBoundChangedRule(),
        new TypeChangedRule(),
        new SafeTypeChangedRule(),
        new ClassDeletedRule(),
        new SafeClassDeletedRule(),
        new SuperTypeDeletedRule(),
        new SuperTypeAddedRule(),
        new SafeSuperTypeDeletedRule(),
        new EnumDeletedRule(),
        new SafeEnumDeletedRule(),
        new EnumLiteralAddedRule(),
        new EnumLiteralDeletedRule(),
        new UniqueChangedRule(),
        new SafeUniqueChangedRule(),
        new ContainmentChangedRule(),
        new AbstractChangedRule(),
        new SafeAbstractChangedRule(),
        new OppositeChangedRule(),
        new PackageAddedRule(),
        new ClassAddedRule(),
        new EnumAddedRule(),
        new PackageDeletedRule(),
        new SafePackageDeletedRule()
    );

    /**
     * Validates whether the given view type conforms to the reference metamodel.
     * Runs Phase I and Phase II in sequence.
     *
     * @param viewType the generated view type
     * @param reference the reference metamodel
     * @param strictness the selected strictness which decides the rules that are applied.
     * @return the violations found, empty if the view type conforms
     */
    public List<Violation> validatePackages(EPackage viewType, EPackage reference, Strictness strictness) {
        List<Diff> diffs = compare(viewType, reference);
        return evaluateDiffs(diffs, strictness);
    }


    /**
     * Phase I: extracts the structural differences between the view type and the reference
     * with EMF Compare.
     *
     * @param viewType the generated view type
     * @param reference the reference metamodel
     * @return the differences of EMF Compare
     */
    private List<Diff> compare(EPackage viewType, EPackage reference) {
        IComparisonScope scope = new DefaultComparisonScope(viewType, reference, null);
        Comparison comparison = EMFCompare.builder().build().compare(scope);
        return comparison.getDifferences();
    }

    /**
     * Phase II: evaluates every difference against the rules of the selected strictness.
     *
     * @param diffs the differences produced by Phase I
     * @param strictness the selected strictness which decides the rules that are applied.
     * @return the violations reported by the evaluated rules
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
}
