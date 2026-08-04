package tools.vitruv.neojoin.conformancevalidator.rules;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;

import tools.vitruv.neojoin.conformancevalidator.Violation;

import java.util.ArrayList;
import java.util.List;

/**
 * R15: DELETE of eSuperType.
 * The view type class does not extend a supertype that the reference class does,
 * but that supertype has no required features, so the view type still conforms.
 */
public class SafeSuperTypeDeletedRule extends ConformanceRule {

    public SafeSuperTypeDeletedRule() {
        super("R15", Violation.Category.NON_BREAKING);
    }

    @Override
    public List<Violation> check(Diff diff) {
        List<Violation> violations = new ArrayList<>();
        if (diff.getKind() == DifferenceKind.DELETE
                && diff instanceof ReferenceChange rc
                && rc.getReference().getName().equals("eSuperTypes")
                && rc.getValue() instanceof EClass supertype) {
            boolean hasRequiredFeatures = supertype.getEAllStructuralFeatures().stream()
                    .anyMatch(sf -> sf.getLowerBound() >= 1);
            if (!hasRequiredFeatures) {
                violations.add(violation(
                    "Supertype '" + supertype.getName() +
                    "' is missing from the view type, but it has no required features in the reference.",
                    diff
                ));
            }
        }
        return violations;
    }
}
