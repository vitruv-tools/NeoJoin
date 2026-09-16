package tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable;

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
 * Rule 29: DELETE of EPackage.
 * The reference declares a subpackage the view type does not have, and it contains a
 * classifier the reference requires.
 * Violation, because an instance of the view type cannot supply that classifier.
 */
public class PackageDeletedRule extends ConformanceRule {

    public PackageDeletedRule() {
        super("Rule 29", Category.BREAKING_NOT_RESOLVABLE);
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
                && RuleUtils.containsRequiredClassifier(deletedPackage)) {
            return Optional.of(violation(
                "Subpackage '" + deletedPackage.getName() + "' of the reference is missing from the "
                    + "view type, and " + RuleUtils.requiredFromOutside(deletedPackage) + ". "
                    + "A query cannot create subpackages, so this requires a change to the reference metamodel.",
                diff
            ));
        }
        return Optional.empty();
    }
}
