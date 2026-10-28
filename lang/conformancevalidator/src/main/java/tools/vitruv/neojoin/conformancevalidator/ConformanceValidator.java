package tools.vitruv.neojoin.conformancevalidator;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EPackage;

import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.Strictness;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable.AbstractChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable.EnumLiteralAddedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable.OppositeChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable.PackageDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable.SuperTypeDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable.ClassDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable.ContainmentChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable.EnumDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable.FeatureDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable.LowerBoundChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable.TypeChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable.UniqueChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable.UpperBoundChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.ClassAddedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.EnumAddedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.EnumLiteralDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.FeatureAddedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.PackageAddedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.SafeAbstractChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.SafeClassDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.SafeEnumDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.SafeFeatureDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.SafeLowerBoundChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.SafePackageDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.SafeSuperTypeDeletedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.SafeTypeChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.SafeUniqueChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.SafeUpperBoundChangedRule;
import tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking.SuperTypeAddedRule;

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
