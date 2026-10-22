package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * Rule 24: CHANGE on abstract.
 * The view type declares the class as abstract, the reference does not.
 * Non-breaking, because the view type then produces fewer direct instances than the
 * reference accepts.
 */
public class SafeAbstractChangedRule extends ConformanceRule {

    public SafeAbstractChangedRule() {
        super("Rule 24", Category.NON_BREAKING);
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
                if (leftClass.isAbstract() && !rightClass.isAbstract()) {
                    return Optional.of(violation(
                        "The view type declares '" + leftClass.getName()
                            + "' as abstract, the reference does not.",
                        diff
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
