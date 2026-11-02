package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EStructuralFeature;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 4: DELETE of EStructuralFeature.
 * The reference declares a feature the view type does not have, and the feature is optional (lowerBound = 0).
 * Non-breaking, because an instance may leave an optional feature empty.
 */
public class SafeFeatureDeletedRule extends ConformanceRule {

    public SafeFeatureDeletedRule() {
        super("Rule 4", Category.NON_BREAKING);
    }

    /**
     * Evaluates the given difference against this rule.
     *
     * @param diff the difference reported by Phase I
     * @return the violation if this rule applies to the difference, an empty optional otherwise
     */
    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.DELETE
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getValue() instanceof EStructuralFeature structuralFeature
                && structuralFeature.getLowerBound() == 0
                && RuleUtils.existsOnBothSides(diff)) {
            return Optional.of(violation(
                "Optional feature '" + RuleUtils.qualifiedName(structuralFeature)
                    + "' of the reference is missing from the view type.",
                diff
            ));
        }
        return Optional.empty();
    }
}
