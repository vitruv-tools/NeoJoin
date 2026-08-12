package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * R14: ADD of eSuperType.
 * The view type class extends a supertype that the reference class does not.
 */
public class SuperTypeAddedRule extends ConformanceRule {

    public SuperTypeAddedRule() {
        super("R14", Violation.Category.NON_BREAKING);
    }

    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.ADD
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getReference().getName().equals("eSuperTypes")
                && referenceChange.getValue() instanceof EClass supertype) {
            return Optional.of(violation(
                "Supertype '" + supertype.getName() + "' is present in the view type but not required by the reference.",
                diff
            ));
        }
        return Optional.empty();
    }
}
