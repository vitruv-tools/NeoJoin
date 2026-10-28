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
 * Rule 26: ADD of EPackage.
 * The view type declares a subpackage the reference does not.
 * Non-breaking, because a consumer written against the reference never reads its classifiers.
 */
public class PackageAddedRule extends ConformanceRule {

    public PackageAddedRule() {
        super("Rule 26", Category.NON_BREAKING);
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
                && referenceChange.getValue() instanceof EPackage addedPackage) {
            return Optional.of(violation(
                "Subpackage '" + addedPackage.getName() + "' is present in the view type "
                    + "but not in the reference.",
                diff
            ));
        }
        return Optional.empty();
    }
}
