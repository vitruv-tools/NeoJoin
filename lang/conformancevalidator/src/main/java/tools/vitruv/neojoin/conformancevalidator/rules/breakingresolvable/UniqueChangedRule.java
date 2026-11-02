package tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable;

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
 * Rule 20: CHANGE on unique.
 * The reference requires unique values, the view type also accepts duplicates.
 * Violation, because a view may then contain duplicates the reference rejects.
 */
public class UniqueChangedRule extends ConformanceRule {

    public UniqueChangedRule() {
        super("Rule 20", Category.BREAKING_RESOLVABLE);
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
                if (rightFeature.isUnique() && !leftFeature.isUnique()) {
                    return Optional.of(violation(
                        "'" + RuleUtils.qualifiedName(leftFeature) + "' allows duplicate values in the view type, "
                            + "the reference requires them to be unique. Add the modifier [unique] to '"
                            + leftFeature.getName() + "' in the query.",
                        diff
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
