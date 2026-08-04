package tools.vitruv.neojoin.conformancevalidator.rules;

import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.ETypedElement;

import tools.vitruv.neojoin.conformancevalidator.Violation;

import java.util.ArrayList;
import java.util.List;

/**
 * R5: CHANGE on lowerBound.
 * Violation if the reference requires more instances than the view type provides.
 */
public class LowerBoundChangedRule extends ConformanceRule {

    public LowerBoundChangedRule() {
        super("R5", Violation.Category.BREAKING_RESOLVABLE);
    }

    @Override
    public List<Violation> check(Diff diff) {
        List<Violation> violations = new ArrayList<>();
        if (diff instanceof AttributeChange ac
                && ac.getAttribute().getName().equals("lowerBound")) {
            EObject viewtype  = ac.getMatch().getLeft();
            EObject reference = ac.getMatch().getRight();
            if (viewtype instanceof ETypedElement leftFeature && reference instanceof ETypedElement rightFeature) {
                if (rightFeature.getLowerBound() > leftFeature.getLowerBound()) {
                    violations.add(violation(
                        "The reference requires more values of '" + leftFeature.getName() +
                        "' than the view type does.",
                        diff
                    ));
                }
            }
        }
        return violations;
    }
}
