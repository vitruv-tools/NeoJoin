package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EEnumLiteral;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * R19: DELETE of EEnumLiteral.
 * The reference declares a literal the view type does not have.
 * Conforming because the view type never produces that value.
 */
public class EnumLiteralDeletedRule extends ConformanceRule {

    public EnumLiteralDeletedRule() {
        super("R19", Violation.Category.NON_BREAKING);
    }

    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.DELETE
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getValue() instanceof EEnumLiteral deletedLiteral) {
            return Optional.of(violation(
                "Literal '" + deletedLiteral.getName() + "' is missing from the view type, "
                    + "but the reference metamodel does not require it.",
                diff
            ));
        }
        return Optional.empty();
    }
}
