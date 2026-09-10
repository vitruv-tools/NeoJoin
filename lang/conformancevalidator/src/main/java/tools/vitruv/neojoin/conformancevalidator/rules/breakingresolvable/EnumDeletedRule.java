package tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable;

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
 * R16: DELETE of EEnum.
 * The reference defines an enum the view type does not have.
 * Violation because a feature in the reference requires that enum (lowerBound >= 1).
 */
public class EnumDeletedRule extends ConformanceRule {

    public EnumDeletedRule() {
        super("R16", Violation.Category.BREAKING_RESOLVABLE);
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

            if (isRequiredSomewhere) {
                return Optional.of(violation(
                    "Enum '" + deletedEnum.getName() + "' is missing from the view type, "
                        + "but the reference metamodel requires it.",
                    diff
                ));
            }
        }
        return Optional.empty();
    }
}
