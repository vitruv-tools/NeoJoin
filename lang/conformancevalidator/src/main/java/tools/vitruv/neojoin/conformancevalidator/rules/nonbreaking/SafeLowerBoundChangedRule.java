package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.ETypedElement;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 6: CHANGE on lowerBound.
 * The view type requires at least as many values as the reference does.
 * Non-breaking, because every instance of the view type satisfies the lowerbound of the reference.
 */
public class SafeLowerBoundChangedRule extends ConformanceRule {

    public SafeLowerBoundChangedRule() {
        super("Rule 6", Category.NON_BREAKING);
    }

    /**
     * Evaluates the given difference against this rule.
     *
     * @param diff the difference reported by Phase I
     * @return the violation if this rule applies to the difference, an empty optional otherwise
     */
    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff instanceof AttributeChange attributeChange
                && attributeChange.getAttribute().getName().equals("lowerBound")) {
            EObject viewtype  = attributeChange.getMatch().getLeft();
            EObject reference = attributeChange.getMatch().getRight();
            if (viewtype instanceof ETypedElement leftFeature && reference instanceof ETypedElement rightFeature) {
                if (rightFeature.getLowerBound() <= leftFeature.getLowerBound()) {
                    return Optional.of(violation(
                        "'" + RuleUtils.qualifiedName(leftFeature) + "' has a lower bound of "
                            + leftFeature.getLowerBound() + " in the view type, where the reference "
                            + "has " + rightFeature.getLowerBound() + ".",
                        diff
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
