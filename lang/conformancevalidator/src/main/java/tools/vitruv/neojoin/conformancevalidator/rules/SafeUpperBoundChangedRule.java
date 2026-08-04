package tools.vitruv.neojoin.conformancevalidator.rules;

import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.ETypedElement;

import tools.vitruv.neojoin.conformancevalidator.Violation;

import java.util.ArrayList;
import java.util.List;

/**
 * R8: CHANGE on upperBound.
 * Conforming if the view type allows the same or fewer values than the reference does.
 * upperBound = -1 means unbounded.
 */
public class SafeUpperBoundChangedRule extends ConformanceRule {

    public SafeUpperBoundChangedRule() {
        super("R8", Violation.Category.NON_BREAKING);
    }

    @Override
    public List<Violation> check(Diff diff) {
        List<Violation> violations = new ArrayList<>();
        if (diff instanceof AttributeChange ac
                && ac.getAttribute().getName().equals("upperBound")) {
            EObject viewtype  = ac.getMatch().getLeft();
            EObject reference = ac.getMatch().getRight();
            if (viewtype instanceof ETypedElement leftFeature && reference instanceof ETypedElement rightFeature) {
                int viewUpper = leftFeature.getUpperBound();
                int refUpper  = rightFeature.getUpperBound();
                boolean isSafe = refUpper == -1 || (viewUpper != -1 && viewUpper <= refUpper);
                if (isSafe) {
                    violations.add(violation(
                        "The view type allows the same or fewer values of '" + leftFeature.getName() +
                        "' than the reference does.",
                        diff
                    ));
                }
            }
        }
        return violations;
    }
}
