package tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.ArrayList;
import java.util.List;

/**
 * R13: DELETE of eSuperType.
 * The view type class does not extend a supertype that the reference class does.
 * Violation if the supertype has required features that the view type class doesn't.
 */
public class SuperTypeDeletedRule extends ConformanceRule {

    public SuperTypeDeletedRule() {
        super("R13", Violation.Category.BREAKING_NOT_RESOLVABLE);
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
            if (hasRequiredFeatures) {
                // Eventuell noch prüfen, ob die View-Type-Klasse diese Felder
                // direkt selbst hat (flache Vererbung). Wird aktuell trotzdem gemeldet.
                violations.add(violation(
                    "Supertype '" + supertype.getName() +
                    "' is missing from the view type but has required inherited features in the reference.",
                    diff
                ));
            }
        }
        return violations;
    }
}
