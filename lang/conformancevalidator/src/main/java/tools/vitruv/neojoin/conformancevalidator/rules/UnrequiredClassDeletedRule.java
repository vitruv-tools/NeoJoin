package tools.vitruv.neojoin.conformancevalidator.rules;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import tools.vitruv.neojoin.conformancevalidator.Violation;

import java.util.ArrayList;
import java.util.List;

/**
 * R12: DELETE of EClass.
 * The reference defines a class the view type does not have, but no class in the
 * reference requires it, so the view type still conforms.
 */
public class UnrequiredClassDeletedRule extends ConformanceRule {

    public UnrequiredClassDeletedRule() {
        super("R12", Violation.Category.NON_BREAKING);
    }

    @Override
    public List<Violation> check(Diff diff) {
        List<Violation> violations = new ArrayList<>();

        if (diff.getKind() == DifferenceKind.DELETE
                && diff instanceof ReferenceChange rc
                && rc.getValue() instanceof EClass deletedClass
                && rc.getMatch().getRight() instanceof EPackage referencePackage) {

            boolean isRequiredSomewhere = referencePackage.getEClassifiers().stream()
                .filter(EClass.class::isInstance)
                .map(EClass.class::cast)
                .flatMap(c -> c.getEAllStructuralFeatures().stream())
                .filter(EReference.class::isInstance)
                .map(EReference.class::cast)
                .anyMatch(ref -> ref.getEType() == deletedClass && ref.getLowerBound() >= 1);

            if (!isRequiredSomewhere) {
                violations.add(violation(
                    "Class '" + deletedClass.getName() + "' is missing from the view type, "
                        + "but the reference metamodel does not require it.",
                    diff
                ));
            }
        }
        return violations;
    }
}
