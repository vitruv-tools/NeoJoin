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
 * Rule 15: DELETE of eSuperType.
 * The view type class does not extend a supertype that the reference class does, and that
 * supertype declares no mandatory feature.
 * Non-breaking, because instances of the view type class carry everything the reference
 * requires of them.
 */
public class SafeSuperTypeDeletedRule extends ConformanceRule {

    public SafeSuperTypeDeletedRule() {
        super("Rule 15", Category.NON_BREAKING);
    }

    /**
     * Evaluates the given difference against this rule.
     *
     * @param diff the difference reported by Phase I
     * @return the violation if this rule applies to the difference, an empty optional otherwise
     */
    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.DELETE
                && RuleUtils.existsOnBothSides(diff)
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getReference().getName().equals("eSuperTypes")
                && referenceChange.getValue() instanceof EClass supertype
                && referenceChange.getMatch().getLeft() instanceof EClass ownerClass) {
            boolean hasRequiredFeatures = supertype.getEAllStructuralFeatures().stream()
                    .anyMatch(sf -> sf.getLowerBound() >= 1);
            if (!hasRequiredFeatures) {
                return Optional.of(violation(
                    "Class '" + ownerClass.getName() + "' does not extend '" + supertype.getName()
                        + "' in the view type, and '" + supertype.getName()
                        + "' declares no mandatory feature.",
                    diff
                ));
            }
        }
        return Optional.empty();
    }
}
