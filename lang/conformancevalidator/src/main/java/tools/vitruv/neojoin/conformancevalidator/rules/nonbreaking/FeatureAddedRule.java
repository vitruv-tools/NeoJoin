package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EStructuralFeature;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * R2: ADD of EStructuralFeature.
 * The view type has a feature the reference does not require.
 */
public class FeatureAddedRule extends ConformanceRule {

    public FeatureAddedRule() {
        super("R2", Violation.Category.NON_BREAKING);
    }

    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.ADD
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getValue() instanceof EStructuralFeature structuralFeature) {
            return Optional.of(violation(
                "Feature '" + structuralFeature.getName() + "' is present in the view type but not required by the reference.",
                diff
            ));
        }
        return Optional.empty();
    }
}
