package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 14: ADD of eSuperType.
 * The view type class extends a supertype that the reference class does not.
 * Non-breaking, because the inherited features are additional structure the reference never reads.
 */
public class SuperTypeAddedRule extends ConformanceRule {

    public SuperTypeAddedRule() {
        super("Rule 14", Category.NON_BREAKING);
    }

    /**
     * Evaluates the given difference against this rule.
     *
     * @param diff the difference reported by Phase I
     * @return the violation if this rule applies to the difference, an empty optional otherwise
     */
    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.ADD
                && RuleUtils.existsOnBothSides(diff)
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getReference().getName().equals("eSuperTypes")
                && referenceChange.getValue() instanceof EClass supertype
                && referenceChange.getMatch().getLeft() instanceof EClass ownerClass) {
            return Optional.of(violation(
                "Class '" + ownerClass.getName() + "' extends '" + supertype.getName()
                    + "' in the view type, which the reference does not declare.",
                diff
            ));
        }
        return Optional.empty();
    }
}
