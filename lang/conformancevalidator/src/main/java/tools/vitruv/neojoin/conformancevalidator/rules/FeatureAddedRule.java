package tools.vitruv.neojoin.conformancevalidator.rules;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EStructuralFeature;

import tools.vitruv.neojoin.conformancevalidator.Violation;

import java.util.ArrayList;
import java.util.List;

/**
 * R2: ADD of EStructuralFeature.
 * The view type has a feature the reference does not require. Extra structure
 * beyond what the reference demands does not violate conformance.
 */
public class FeatureAddedRule extends ConformanceRule {

    public FeatureAddedRule() {
        super("R2", Violation.Category.NON_BREAKING);
    }

    @Override
    public List<Violation> check(Diff diff) {
        List<Violation> violations = new ArrayList<>();
        if (diff.getKind() == DifferenceKind.ADD
                && diff instanceof ReferenceChange rc
                && rc.getValue() instanceof EStructuralFeature sf) {
            violations.add(violation(
                "Feature '" + sf.getName() + "' is present in the view type but not required by the reference.",
                diff
            ));
        }
        return violations;
    }
}
