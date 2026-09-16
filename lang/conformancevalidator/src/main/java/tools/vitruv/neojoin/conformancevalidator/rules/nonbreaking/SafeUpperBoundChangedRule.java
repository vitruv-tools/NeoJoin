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
 * Rule 8: CHANGE on upperBound.
 * The view type allows the same number of values as the reference or fewer.
 * Non-breaking, because every view instance stays within the bounds of the reference.
 */
public class SafeUpperBoundChangedRule extends ConformanceRule {

    public SafeUpperBoundChangedRule() {
        super("Rule 8", Category.NON_BREAKING);
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
                && attributeChange.getAttribute().getName().equals("upperBound")) {
            EObject viewtype  = attributeChange.getMatch().getLeft();
            EObject reference = attributeChange.getMatch().getRight();
            if (viewtype instanceof ETypedElement leftFeature && reference instanceof ETypedElement rightFeature
                    && !RuleUtils.higherUpperBound(leftFeature.getUpperBound(), rightFeature.getUpperBound())) {
                return Optional.of(violation(
                    "'" + RuleUtils.qualifiedName(leftFeature) + "' has an upper bound of "
                        + RuleUtils.bound(leftFeature.getUpperBound()) + " in the view type, where the "
                        + "reference has " + RuleUtils.bound(rightFeature.getUpperBound()) + ".",
                    diff
                ));
            }
        }
        return Optional.empty();
    }
}
