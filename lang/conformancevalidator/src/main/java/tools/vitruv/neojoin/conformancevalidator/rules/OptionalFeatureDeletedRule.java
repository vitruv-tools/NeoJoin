package tools.vitruv.neojoin.conformancevalidator.rules;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EStructuralFeature;

import tools.vitruv.neojoin.conformancevalidator.Violation;

import java.util.ArrayList;
import java.util.List;

/**
 * R4: DELETE of EStructuralFeature.
 * The reference has a feature the view type does not, but the feature is optional
 * (lowerBound = 0), so the view type still conforms.
 */
public class OptionalFeatureDeletedRule extends ConformanceRule {

    public OptionalFeatureDeletedRule() {
        super("R4", Violation.Category.NON_BREAKING);
    }

    @Override
    public List<Violation> check(Diff diff) {
        List<Violation> violations = new ArrayList<>();
        if (diff.getKind() == DifferenceKind.DELETE
                && diff instanceof ReferenceChange rc
                && rc.getValue() instanceof EStructuralFeature sf
                && sf.getLowerBound() == 0) {
            violations.add(violation(
                "Optional feature '" + sf.getName() +
                "' is present in the reference but missing from the view type.",
                diff
            ));
        }
        return violations;
    }
}
