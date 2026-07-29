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
 * R9/R10: DELETE of EClass.
 * The reference defines a class the view type does not have.
 * Violation only if that class is required somewhere in the reference (lowerBound >= 1).
 */
public class RequiredClassDeletedRule extends ConformanceRule {

    public RequiredClassDeletedRule() {
        super("R9", Violation.Category.BREAKING_NOT_RESOLVABLE);
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

            
            if (isRequiredSomewhere) {
                violations.add(violation(
                    "Class '" + deletedClass.getName() + "' is required by a reference in the "
                        + "reference metamodel but missing from the view type.",
                    diff
                ));
            }
        }
        return violations;
    }
}
