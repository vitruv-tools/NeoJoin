package tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable;

import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * Rule 23: CHANGE on abstract.
 * The reference declares the class as abstract, the view type allows direct instances of it.
 * Violation, because the view type then allos instances the reference rejects.
 */
public class AbstractChangedRule extends ConformanceRule {

    public AbstractChangedRule() {
        super("Rule 23", Category.BREAKING_NOT_RESOLVABLE);
    }

    /**
     * Evaluates the given difference against this rule.
     *
     * @param diff the difference reported by Phase I
     * @return the violation if this rule applies to the difference, an empty optional otherwise
     */
    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff instanceof AttributeChange attributeChange
                && attributeChange.getAttribute().getName().equals("abstract")) {
            EObject viewtype  = attributeChange.getMatch().getLeft();
            EObject reference = attributeChange.getMatch().getRight();
            if (viewtype instanceof EClass leftClass && reference instanceof EClass rightClass) {
                if (rightClass.isAbstract() && !leftClass.isAbstract()) {
                    return Optional.of(violation(
                        "The reference declares '" + rightClass.getName() + "' as abstract, "
                            + "the view type allows direct instances of it. "
                            + "A query cannot declare a class abstract, so this requires a change "
                            + "to the reference metamodel.",
                        diff
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
