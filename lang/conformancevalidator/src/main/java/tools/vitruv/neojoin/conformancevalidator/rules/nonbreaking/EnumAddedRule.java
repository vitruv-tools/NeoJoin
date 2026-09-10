package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EEnum;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * R28: ADD of EEnum.
 * The view type has an enum the reference does not require.
 */
public class EnumAddedRule extends ConformanceRule {

    public EnumAddedRule() {
        super("R28", Violation.Category.NON_BREAKING);
    }

    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.ADD
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getValue() instanceof EEnum addedEnum) {
            return Optional.of(violation(
                "Enum '" + addedEnum.getName() + "' is present in the view type "
                    + "but not required by the reference.",
                diff
            ));
        }
        return Optional.empty();
    }
}
