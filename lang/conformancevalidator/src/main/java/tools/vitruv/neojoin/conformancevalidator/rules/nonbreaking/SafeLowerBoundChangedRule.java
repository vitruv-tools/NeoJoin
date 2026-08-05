package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.ETypedElement;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.ArrayList;
import java.util.List;

/**
 * R6: CHANGE on lowerBound.
 * Conforming if the reference requires the same or fewer instances than the view type provides.
 */
public class SafeLowerBoundChangedRule extends ConformanceRule {

    public SafeLowerBoundChangedRule() {
        super("R6", Violation.Category.NON_BREAKING);
    }

    @Override
    public List<Violation> check(Diff diff) {
        List<Violation> violations = new ArrayList<>();
        if (diff instanceof AttributeChange ac
                && ac.getAttribute().getName().equals("lowerBound")) {
            EObject viewtype  = ac.getMatch().getLeft();
            EObject reference = ac.getMatch().getRight();
            if (viewtype instanceof ETypedElement leftFeature && reference instanceof ETypedElement rightFeature) {
                if (rightFeature.getLowerBound() <= leftFeature.getLowerBound()) {
                    violations.add(violation(
                        "The reference requires the same or fewer values of '" + leftFeature.getName() +
                        "' than the view type does.",
                        diff
                    ));
                }
            }
        }
        return violations;
    }
}
