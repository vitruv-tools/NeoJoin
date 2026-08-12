package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.ETypedElement;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

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
    public Optional<Violation> check(Diff diff) {
        if (diff instanceof AttributeChange attributeChange
                && attributeChange.getAttribute().getName().equals("upperBound")) {
            EObject viewtype  = attributeChange.getMatch().getLeft();
            EObject reference = attributeChange.getMatch().getRight();
            if (viewtype instanceof ETypedElement leftFeature && reference instanceof ETypedElement rightFeature) {
                int viewUpper = leftFeature.getUpperBound();
                int refUpper  = rightFeature.getUpperBound();
                boolean isSafe = refUpper == -1 || (viewUpper != -1 && viewUpper <= refUpper);
                if (isSafe) {
                    return Optional.of(violation(
                        "The view type allows the same or fewer values of '" + leftFeature.getName() +
                        "' than the reference does.",
                        diff
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
