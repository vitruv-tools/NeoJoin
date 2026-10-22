package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 27: ADD of EClass.
 * The view type declares a class the reference does not.
 * Non-breaking, because a consumer written against the reference never reads that class.
 */
public class ClassAddedRule extends ConformanceRule {

    public ClassAddedRule() {
        super("Rule 27", Category.NON_BREAKING);
    }

    /**
     * Evaluates the given difference against this rule.
     *
     * @param diff the difference reported by Phase I
     * @return the violation if this rule applies to the difference, an empty optional otherwise
     */
    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.ADD
                && RuleUtils.existsOnBothSides(diff)
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getReference().getName().equals("eClassifiers")
                && referenceChange.getValue() instanceof EClass addedClass) {
            return Optional.of(violation(
                "Class '" + addedClass.getName() + "' is present in the view type "
                    + "but not declared by the reference.",
                diff
            ));
        }
        return Optional.empty();
    }
}
