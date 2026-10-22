package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EStructuralFeature;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 2: ADD of EStructuralFeature.
 * The view type declares a feature the reference does not.
 * Non-breaking, because a consumer written against the reference reads only what the reference declares.
 */
public class FeatureAddedRule extends ConformanceRule {

    public FeatureAddedRule() {
        super("Rule 2", Category.NON_BREAKING);
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
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getValue() instanceof EStructuralFeature structuralFeature
                && RuleUtils.existsOnBothSides(diff)) {
            return Optional.of(violation(
                "Feature '" + RuleUtils.qualifiedName(structuralFeature)
                    + "' is present in the view type but not declared by the reference.",
                diff
            ));
        }
        return Optional.empty();
    }
}
