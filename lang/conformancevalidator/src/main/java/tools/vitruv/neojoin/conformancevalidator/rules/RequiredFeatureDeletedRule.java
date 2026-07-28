package tools.vitruv.neojoin.conformancevalidator.rules;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EStructuralFeature;

import tools.vitruv.neojoin.conformancevalidator.Violation;

import java.util.ArrayList;
import java.util.List;

/**
 * R3/R4: DELETE of EStructuralFeature.
 * The reference has a feature the view type does not.
 * Violation only if the feature is required (lowerBound >= 1).
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
                && rc.getValue() instanceof EStructuralFeature sf) {
            if (sf.getLowerBound() >= 1) {
                violations.add(violation(
                    "Required feature '" + sf.getName() + "' (lowerBound=" + sf.getLowerBound() + ")" +
                    " is present in the reference but missing from the view type.",
                    diff
                ));
            }
            // R4: lowerBound = 0 -> not a violation
        }
        return violations;
    }
}
