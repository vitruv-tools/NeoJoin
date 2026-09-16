package tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable;

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
 * Rule 13: DELETE of eSuperType.
 * The view type class does not extend a supertype that the reference class does, and that
 * supertype declares a required feature.
 * Violation, because instances of the view type class would not carry that feature.
 */
public class SuperTypeDeletedRule extends ConformanceRule {

    public SuperTypeDeletedRule() {
        super("Rule 13", Category.BREAKING_NOT_RESOLVABLE);
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
                && referenceChange.getReference().getName().equals("eSuperTypes")
                && referenceChange.getValue() instanceof EClass supertype
                && referenceChange.getMatch().getLeft() instanceof EClass ownerClass) {
            var required = supertype.getEAllStructuralFeatures().stream()
                    .filter(sf -> sf.getLowerBound() >= 1)
                    .findFirst();
            if (required.isPresent()) {
                // Eventuell noch prüfen, ob die View-Type-Klasse diese Felder
                // direkt selbst hat (flache Vererbung). Wird aktuell trotzdem gemeldet.
                return Optional.of(violation(
                    "Class '" + ownerClass.getName() + "' does not extend '" + supertype.getName()
                        + "' in the view type, and '" + supertype.getName()
                        + "' declares the mandatory feature '" + required.get().getName()
                        + "' that instances of '" + ownerClass.getName() + "' would have to carry. "
                        + "A query cannot declare supertypes, so this requires a change to the reference metamodel.",
                    diff
                ));
            }
        }
        return Optional.empty();
    }
}
