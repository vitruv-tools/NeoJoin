package tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable;

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
 * Rule 3: DELETE of EStructuralFeature.
 * The reference declares a feature the view type does not have, and the feature is
 * required (lowerBound >= 1).
 * Violation, because an instance of the view type cannot satisfy it.
 */
public class FeatureDeletedRule extends ConformanceRule {

    public FeatureDeletedRule() {
        super("Rule 3", Category.BREAKING_RESOLVABLE);
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
                && structuralFeature.getLowerBound() >= 1
                && RuleUtils.existsOnBothSides(diff)) {
            return Optional.of(violation(
                "Required feature '" + RuleUtils.qualifiedName(structuralFeature) + "' of the reference is "
                    + "missing from the view type, so instances cannot satisfy it. Add '"
                    + structuralFeature.getName() + "' to the query that creates '"
                    + structuralFeature.getEContainingClass().getName() + "'.",
                diff
            ));
        }
        return Optional.empty();
    }
}
