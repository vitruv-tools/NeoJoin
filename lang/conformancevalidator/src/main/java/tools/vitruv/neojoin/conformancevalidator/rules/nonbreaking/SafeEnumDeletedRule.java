package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 17: DELETE of EEnum.
 * The reference declares an enum the view type does not have, and no feature of the reference requires it.
 * Non-breaking, because no instance has to carry a value of it.
 */
public class SafeEnumDeletedRule extends ConformanceRule {

    public SafeEnumDeletedRule() {
        super("Rule 17", Category.NON_BREAKING);
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
                && referenceChange.getValue() instanceof EEnum deletedEnum
                && referenceChange.getMatch().getRight() instanceof EPackage referencePackage
                && !RuleUtils.isRequired(referencePackage, deletedEnum)) {
            return Optional.of(violation(
                "Enumeration '" + deletedEnum.getName() + "' of the reference is missing from the view type, "
                    + "and no feature requires it.",
                diff
            ));
        }
        return Optional.empty();
    }
}
