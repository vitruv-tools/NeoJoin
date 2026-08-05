package tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.ArrayList;
import java.util.List;

/**
 * R11: DELETE of EClass.
 * The reference defines a class the view type does not have.
 * Violation because that class is required somewhere in the reference (lowerBound >= 1).
 */
public class ClassDeletedRule extends ConformanceRule {

    public ClassDeletedRule() {
        super("R11", Violation.Category.BREAKING_NOT_RESOLVABLE);
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
                    "Class '" + deletedClass.getName() + "' is missing from the view type, "
                        + "but the reference metamodel requires it.",
                    diff
                ));
            }
        }
        return violations;
    }
}
