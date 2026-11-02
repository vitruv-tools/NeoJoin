package tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EReference;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 25: CHANGE on eOpposite.
 * The reference declares an opposite for a reference that the view type does not declare.
 * Violation, because the view type does not maintain the bidirectional link.
 */
public class OppositeChangedRule extends ConformanceRule {

    public OppositeChangedRule() {
        super("Rule 25", Category.BREAKING_NOT_RESOLVABLE);
    }

    /**
     * Evaluates the given difference against this rule.
     *
     * @param diff the difference reported by Phase I
     * @return the violation if this rule applies to the difference, an empty optional otherwise
     */
    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff instanceof ReferenceChange referenceChange
                && referenceChange.getReference().getName().equals("eOpposite")
                && referenceChange.getMatch().getLeft() instanceof EReference viewTypeReference
                && referenceChange.getMatch().getRight() instanceof EReference referenceReference
                && viewTypeReference.getEOpposite() == null
                && referenceReference.getEOpposite() != null) {
            return Optional.of(violation(
                "'" + RuleUtils.qualifiedName(viewTypeReference) + "' has no opposite in the view type, "
                    + "while the reference declares '"
                    + RuleUtils.qualifiedName(referenceReference.getEOpposite())
                    + "' as its opposite, so the view does not maintain the link in both directions. "
                    + "A query cannot declare opposites, so this requires a change to the reference metamodel.",
                diff
            ));
        }
        return Optional.empty();
    }
}
