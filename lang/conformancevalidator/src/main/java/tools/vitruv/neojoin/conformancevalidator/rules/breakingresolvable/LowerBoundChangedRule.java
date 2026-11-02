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
 * Rule 5: CHANGE on lowerBound.
 * The reference requires more values than the view type does.
 * Violation, because a view may then leave the feature with fewer values than the
 * reference requires.
 */
public class LowerBoundChangedRule extends ConformanceRule {

    public LowerBoundChangedRule() {
        super("Rule 5", Category.BREAKING_RESOLVABLE);
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
                if (rightFeature.getLowerBound() > leftFeature.getLowerBound()) {
                    return Optional.of(violation(
                        "The reference sets the lower bound of '" + RuleUtils.qualifiedName(leftFeature)
                            + "' to " + rightFeature.getLowerBound() + ", the view type to "
                            + leftFeature.getLowerBound() + ". Raise the multiplicity of '"
                            + leftFeature.getName() + "' in the query, for example with ["
                            + rightFeature.getLowerBound() + ".."
                            + RuleUtils.bound(leftFeature.getUpperBound()) + "].",
                        diff
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
