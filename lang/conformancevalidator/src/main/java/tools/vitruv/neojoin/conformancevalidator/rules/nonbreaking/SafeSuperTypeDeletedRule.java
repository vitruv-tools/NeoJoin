package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * R15: DELETE of eSuperType.
 * The view type class does not extend a supertype that the reference class does,
 * but that supertype has no required features.
 */
public class SafeSuperTypeDeletedRule extends ConformanceRule {

    public SafeSuperTypeDeletedRule() {
        super("R15", Violation.Category.NON_BREAKING);
    }

    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.DELETE
                && diff instanceof ReferenceChange rc
                && rc.getReference().getName().equals("eSuperTypes")
                && rc.getValue() instanceof EClass supertype) {
            boolean hasRequiredFeatures = supertype.getEAllStructuralFeatures().stream()
                    .anyMatch(sf -> sf.getLowerBound() >= 1);
            if (!hasRequiredFeatures) {
                return Optional.of(violation(
                    "Supertype '" + supertype.getName() +
                    "' is missing from the view type, but it has no required features in the reference.",
                    diff
                ));
            }
        }
        return Optional.empty();
    }
}
