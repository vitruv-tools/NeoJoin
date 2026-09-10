package tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EEnumLiteral;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * R18: ADD of EEnumLiteral.
 * The view type declares an enum literal the reference does not have.
 * Violation because a view could assign a value the reference does not accept.
 */
public class EnumLiteralAddedRule extends ConformanceRule {

    public EnumLiteralAddedRule() {
        super("R18", Violation.Category.BREAKING_NOT_RESOLVABLE);
    }

    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.ADD
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getValue() instanceof EEnumLiteral addedLiteral) {
            return Optional.of(violation(
                "Literal '" + addedLiteral.getName() + "' is present in the view type, "
                    + "but not in the reference metamodel.",
                diff
            ));
        }
        return Optional.empty();
    }
}
