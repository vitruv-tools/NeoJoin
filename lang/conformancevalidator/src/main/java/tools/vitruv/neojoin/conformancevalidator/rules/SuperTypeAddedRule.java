package tools.vitruv.neojoin.conformancevalidator.rules;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;

import tools.vitruv.neojoin.conformancevalidator.Violation;

import java.util.ArrayList;
import java.util.List;

/**
 * R14: ADD of eSuperType.
 * The view type class extends a supertype that the reference class does not.
 * Extra structure beyond what the reference demands does not violate conformance.
 */
public class SuperTypeAddedRule extends ConformanceRule {

    public SuperTypeAddedRule() {
        super("R14", Violation.Category.NON_BREAKING);
    }

    @Override
    public List<Violation> check(Diff diff) {
        List<Violation> violations = new ArrayList<>();
        if (diff.getKind() == DifferenceKind.ADD
                && diff instanceof ReferenceChange rc
                && rc.getReference().getName().equals("eSuperTypes")
                && rc.getValue() instanceof EClass supertype) {
            violations.add(violation(
                "Supertype '" + supertype.getName() + "' is present in the view type but not required by the reference.",
                diff
            ));
        }
        return violations;
    }
}
