package tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable;

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
 * Rule 18: ADD of EEnumLiteral.
 * The view type declares a literal the reference does not have.
 * Violation, because a view could assign a value the reference does not accept.S
 */
public class EnumLiteralAddedRule extends ConformanceRule {

    public EnumLiteralAddedRule() {
        super("Rule 18", Category.BREAKING_NOT_RESOLVABLE);
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
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getValue() instanceof EEnumLiteral addedLiteral
                && RuleUtils.existsOnBothSides(diff)) {
            return Optional.of(violation(
                "The view type declares literal '" + RuleUtils.qualifiedName(addedLiteral)
                    + "', which the reference does not accept, so a view could carry a value "
                    + "the reference rejects. The literals of an enumeration are copied from the "
                    + "source metamodel, so this requires a change to the source or to the reference.",
                diff
            ));
        }
        return Optional.empty();
    }
}
