package tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable;

import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 22: CHANGE on containment.
 * The containment of a reference differs between view type and reference.
 * Violation in either direction, because containment decides where the referenced objects
 * are stored, so the structure is different.
 */
public class ContainmentChangedRule extends ConformanceRule {

    public ContainmentChangedRule() {
        super("Rule 22", Category.BREAKING_RESOLVABLE);
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
                && attributeChange.getAttribute().getName().equals("containment")) {
            EObject viewtype  = attributeChange.getMatch().getLeft();
            EObject reference = attributeChange.getMatch().getRight();
            if (viewtype instanceof EReference leftReference && reference instanceof EReference rightReference) {
                String name = RuleUtils.qualifiedName(leftReference);
                if (rightReference.isContainment() && !leftReference.isContainment()) {
                    return Optional.of(violation(
                        "'" + name + "' is a containment in the reference but not in the view type, "
                            + "so a view places the referenced objects elsewhere in the model tree. "
                            + "Add the modifier [containment] to '" + leftReference.getName() + "' in the query.",
                        diff
                    ));
                }
                return Optional.of(violation(
                    "'" + name + "' is a containment in the view type but not in the reference, "
                        + "so a view places the referenced objects inside their container. "
                        + "Remove the modifier [containment] from '" + leftReference.getName() + "' in the query.",
                    diff
                ));
            }
        }
        return Optional.empty();
    }
}
