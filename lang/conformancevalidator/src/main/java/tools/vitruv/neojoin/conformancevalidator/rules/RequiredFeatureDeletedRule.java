package tools.vitruv.neojoin.conformancevalidator.rules;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EStructuralFeature;

import tools.vitruv.neojoin.conformancevalidator.Violation;

import java.util.ArrayList;
import java.util.List;

/**
 * R3: DELETE of EStructuralFeature.
 * The reference has a feature the view type does not.
 * Violation because the feature is required (lowerBound >= 1).
 */
public class RequiredFeatureDeletedRule extends ConformanceRule {

    public RequiredFeatureDeletedRule() {
        super("R3", Violation.Category.BREAKING_RESOLVABLE);
    }

    @Override
    public List<Violation> check(Diff diff) {
        List<Violation> violations = new ArrayList<>();
        if (diff.getKind() == DifferenceKind.DELETE
                && diff instanceof ReferenceChange rc
                && rc.getValue() instanceof EStructuralFeature sf
                && sf.getLowerBound() >= 1) {
            violations.add(violation(
                "Required feature '" + sf.getName() +
                "' is present in the reference but missing from the view type.",
                diff
            ));
        }
        return violations;
    }
}
