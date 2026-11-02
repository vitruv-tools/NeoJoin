package tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 16: DELETE of EEnum.
 * The reference declares an enum the view type does not have, and a feature of the
 * reference requires it (lowerBound >= 1).
 * Violation, because an instance of the view type cannot supply a value for that feature.
 */
public class EnumDeletedRule extends ConformanceRule {

    public EnumDeletedRule() {
        super("Rule 16", Category.BREAKING_RESOLVABLE);
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
                && referenceChange.getValue() instanceof EEnum deletedEnum
                && referenceChange.getMatch().getRight() instanceof EPackage referencePackage
                && RuleUtils.isRequired(referencePackage, deletedEnum)) {
            return Optional.of(violation(
                "Enumeration '" + deletedEnum.getName() + "' of the reference is missing from the view type "
                    + "and is required by '"
                    + RuleUtils.requiringFeatureName(referencePackage, deletedEnum)
                    + "'. Select a feature typed '" + deletedEnum.getName() + "' in the query so "
                    + "that the enumeration is copied into the view type.",
                diff
            ));
        }
        return Optional.empty();
    }
}
