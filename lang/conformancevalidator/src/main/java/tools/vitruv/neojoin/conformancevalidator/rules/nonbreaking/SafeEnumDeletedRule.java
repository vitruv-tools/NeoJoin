package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * R17: DELETE of EEnum.
 * The reference defines an enum the view type does not have, but no feature in the
 * reference requires it.
 */
public class SafeEnumDeletedRule extends ConformanceRule {

    public SafeEnumDeletedRule() {
        super("R17", Violation.Category.NON_BREAKING);
    }

    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.DELETE
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getValue() instanceof EEnum deletedEnum
                && referenceChange.getMatch().getRight() instanceof EPackage referencePackage) {

            boolean isRequiredSomewhere = referencePackage.getEClassifiers().stream()
                .filter(EClass.class::isInstance)
                .map(EClass.class::cast)
                .flatMap(c -> c.getEAllStructuralFeatures().stream())
                .anyMatch(f -> f.getEType() == deletedEnum && f.getLowerBound() >= 1);

            if (!isRequiredSomewhere) {
                return Optional.of(violation(
                    "Enum '" + deletedEnum.getName() + "' is missing from the view type, "
                        + "but the reference metamodel does not require it.",
                    diff
                ));
            }
        }
        return Optional.empty();
    }
}
