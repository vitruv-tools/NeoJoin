package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EStructuralFeature;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * R4: DELETE of EStructuralFeature.
 * The reference has a feature the view type does not, but the feature is optional (lowerBound = 0).
 */
public class SafeFeatureDeletedRule extends ConformanceRule {

    public SafeFeatureDeletedRule() {
        super("R4", Violation.Category.NON_BREAKING);
    }

    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff.getKind() == DifferenceKind.DELETE
                && diff instanceof ReferenceChange referenceChange
                && referenceChange.getValue() instanceof EStructuralFeature structuralFeature
                && structuralFeature.getLowerBound() == 0) {
            return Optional.of(violation(
                "Optional feature '" + structuralFeature.getName() +
                "' is present in the reference but missing from the view type.",
                diff
            ));
        }
        return Optional.empty();
    }
}
