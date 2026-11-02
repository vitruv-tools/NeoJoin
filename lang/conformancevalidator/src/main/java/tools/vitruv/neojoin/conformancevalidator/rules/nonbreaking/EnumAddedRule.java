package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EEnum;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 28: ADD of EEnum.
 * The view type declares an enum the reference does not.
 * Non-breaking, because a consumer written against the reference never reads that enumeration.
 */
public class EnumAddedRule extends ConformanceRule {

    public EnumAddedRule() {
        super("Rule 28", Category.NON_BREAKING);
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
                && referenceChange.getValue() instanceof EEnum addedEnum) {
            return Optional.of(violation(
                "Enumeration '" + addedEnum.getName() + "' is present in the view type "
                    + "but not declared by the reference.",
                diff
            ));
        }
        return Optional.empty();
    }
}
