package tools.vitruv.neojoin.conformancevalidator.rules;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;

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
                && rc.getValue() instanceof EClass) {
            // TODO (R9): check all EReferences in the reference EPackage
            // whether any of them has this class as eType and lowerBound >= 1.
            // If yes: violation. If all references are optional (lowerBound = 0): conforming (R10).
        }
        return violations;
    }
}
