package tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable;

import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.ETypedElement;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * R7: CHANGE on upperBound.
 * Violation if the reference allows fewer values than the view type produces.
 * upperBound = -1 means unbounded.
 */
public class UpperBoundChangedRule extends ConformanceRule {

    public UpperBoundChangedRule() {
        super("R7", Violation.Category.BREAKING_RESOLVABLE);
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
                boolean isViolation = refUpper != -1 && (viewUpper == -1 || viewUpper > refUpper);
                if (isViolation) {
                    return Optional.of(violation(
                        "The view type allows more values of '" + leftFeature.getName() +
                        "' than the reference does.",
                        diff
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
