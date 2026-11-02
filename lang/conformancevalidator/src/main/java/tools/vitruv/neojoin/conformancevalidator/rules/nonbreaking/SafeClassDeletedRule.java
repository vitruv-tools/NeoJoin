package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 12: DELETE of EClass.
 * The reference declares a class the view type does not have, and no feature of the reference requires it.
 * Non-breaking, because no instance has to contain one.
 */
public class SafeClassDeletedRule extends ConformanceRule {

    public SafeClassDeletedRule() {
        super("Rule 12", Category.NON_BREAKING);
    }

    /**
     * Evaluates the given difference against this rule.
     *
     * @param diff the difference reported by Phase I
     * @return the violation if this rule applies to the difference, an empty optional otherwise
     */
    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.DELETE
                && RuleUtils.existsOnBothSides(diff)
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getValue() instanceof EClass deletedClass
                && referenceChange.getMatch().getRight() instanceof EPackage referencePackage
                && !RuleUtils.isRequired(referencePackage, deletedClass)) {
            return Optional.of(violation(
                "Class '" + deletedClass.getName() + "' of the reference is missing from the view type, "
                    + "and no feature requires it.",
                diff
            ));
        }
        return Optional.empty();
    }
}
