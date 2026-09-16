package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EEnumLiteral;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 19: DELETE of EEnumLiteral.
 * The reference declares a literal the view type does not have.
 * Non-breaking, because the view type never produces that value.
 */
public class EnumLiteralDeletedRule extends ConformanceRule {

    public EnumLiteralDeletedRule() {
        super("Rule 19", Category.NON_BREAKING);
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
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getValue() instanceof EEnumLiteral deletedLiteral
                && RuleUtils.existsOnBothSides(diff)) {
            return Optional.of(violation(
                "Literal '" + RuleUtils.qualifiedName(deletedLiteral)
                    + "' is missing from the view type.",
                diff
            ));
        }
        return Optional.empty();
    }
}
