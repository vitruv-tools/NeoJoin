package tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable;

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
 * Rule 11: DELETE of EClass.
 * The reference declares a class the view type does not have, and a feature of the
 * reference requires it (lowerBound >= 1).
 * Violation, because an instance of the view type cannot fill that feature.
 */
public class ClassDeletedRule extends ConformanceRule {

    public ClassDeletedRule() {
        super("Rule 11", Category.BREAKING_RESOLVABLE);
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
                && RuleUtils.isRequired(referencePackage, deletedClass)) {
            return Optional.of(violation(
                "Class '" + deletedClass.getName() + "' of the reference is missing from the view type "
                    + "and is required by '"
                    + RuleUtils.requiringFeatureName(referencePackage, deletedClass)
                    + "'. Add a query that creates '" + deletedClass.getName() + "'.",
                diff
            ));
        }
        return Optional.empty();
    }
}
