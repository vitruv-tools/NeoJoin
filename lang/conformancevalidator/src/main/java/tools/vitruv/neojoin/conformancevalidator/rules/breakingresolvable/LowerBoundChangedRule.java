package tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable;

import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.ETypedElement;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * R5: CHANGE on lowerBound.
 * Violation if the reference requires more instances than the view type provides.
 */
public class LowerBoundChangedRule extends ConformanceRule {

    public LowerBoundChangedRule() {
        super("R5", Violation.Category.BREAKING_RESOLVABLE);
    }

    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff instanceof AttributeChange attributeChange
                && attributeChange.getAttribute().getName().equals("lowerBound")) {
            EObject viewtype  = attributeChange.getMatch().getLeft();
            EObject reference = attributeChange.getMatch().getRight();
            if (viewtype instanceof ETypedElement leftFeature && reference instanceof ETypedElement rightFeature) {
                if (rightFeature.getLowerBound() > leftFeature.getLowerBound()) {
                    return Optional.of(violation(
                        "The reference requires more values of '" + leftFeature.getName() +
                        "' than the view type does.",
                        diff
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
