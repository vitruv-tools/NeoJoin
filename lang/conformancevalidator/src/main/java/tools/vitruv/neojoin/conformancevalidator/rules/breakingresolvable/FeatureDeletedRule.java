package tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EStructuralFeature;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * R3: DELETE of EStructuralFeature.
 * The reference has a feature the view type does not.
 * Violation because the feature is required (lowerBound >= 1).
 */
public class FeatureDeletedRule extends ConformanceRule {

    public FeatureDeletedRule() {
        super("R3", Violation.Category.BREAKING_RESOLVABLE);
    }

    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.DELETE
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getValue() instanceof EStructuralFeature structuralFeature
                && structuralFeature.getLowerBound() >= 1) {
            return Optional.of(violation(
                "Required feature '" + structuralFeature.getName() +
                "' is present in the reference but missing from the view type.",
                diff
            ));
        }
        return Optional.empty();
    }
}
