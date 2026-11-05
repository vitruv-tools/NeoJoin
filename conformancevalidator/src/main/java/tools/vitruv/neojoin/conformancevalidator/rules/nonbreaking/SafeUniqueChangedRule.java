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
 * Rule 21: CHANGE on unique.
 * The view type requires unique values, the reference also accepts duplicates.
 * Non-breaking, because the view type then produces fewer values than the reference accepts.
 */
public class SafeUniqueChangedRule extends ConformanceRule {

    public SafeUniqueChangedRule() {
        super("Rule 21", Category.NON_BREAKING);
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
                && attributeChange.getAttribute().getName().equals("unique")) {
            EObject viewtype  = attributeChange.getMatch().getLeft();
            EObject reference = attributeChange.getMatch().getRight();
            if (viewtype instanceof ETypedElement leftFeature && reference instanceof ETypedElement rightFeature) {
                if (leftFeature.isUnique() && !rightFeature.isUnique()) {
                    return Optional.of(violation(
                        "'" + RuleUtils.qualifiedName(leftFeature) + "' requires unique values in the view type, "
                            + "while the reference also accepts duplicates.",
                        diff
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
