package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EPackage;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 30: DELETE of EPackage.
 * The reference declares a subpackage the view type does not have, and no feature outside
 * it requires one of its classifiers.
 * Non-breaking, because no instance has to contain one of them.
 */
public class SafePackageDeletedRule extends ConformanceRule {

    public SafePackageDeletedRule() {
        super("Rule 30", Category.NON_BREAKING);
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
                && referenceChange.getValue() instanceof EPackage deletedPackage
                && !RuleUtils.containsRequiredClassifier(deletedPackage)) {
            return Optional.of(violation(
                "Subpackage '" + deletedPackage.getName() + "' of the reference is missing from the view type, "
                    + "and no feature requires its classifiers.",
                diff
            ));
        }
        return Optional.empty();
    }
}
