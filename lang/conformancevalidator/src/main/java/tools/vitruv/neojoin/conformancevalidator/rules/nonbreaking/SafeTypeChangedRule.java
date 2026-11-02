package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.ETypedElement;

import tools.vitruv.neojoin.conformancevalidator.Category;
import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.RuleUtils;

import java.util.Optional;

/**
 * Rule 10: CHANGE on eType.
 * The type of a feature differs between view type and reference, and the type of the view
 * type is a subtype of the type of the reference.
 * Non-breaking, because every value the view type can hold is one the reference accepts.
 */
public class SafeTypeChangedRule extends ConformanceRule {

    public SafeTypeChangedRule() {
        super("Rule 10", Category.NON_BREAKING);
    }

    /**
     * Evaluates the given difference against this rule.
     *
     * @param diff the difference reported by Phase I
     * @return the violation if this rule applies to the difference, an empty optional otherwise
     */
    @Override
    public Optional<Violation> check(Diff diff) {
        if (diff instanceof ReferenceChange referenceChange
                && referenceChange.getReference().getName().equals("eType")
                && diff.getKind() == DifferenceKind.CHANGE) {
            EObject viewtype  = referenceChange.getMatch().getLeft();
            EObject reference = referenceChange.getMatch().getRight();
            if (viewtype instanceof ETypedElement leftFeature && reference instanceof ETypedElement rightFeature) {
                EClassifier viewTypeType  = leftFeature.getEType();
                EClassifier referenceType = rightFeature.getEType();
                if (viewTypeType != null && referenceType != null
                        && RuleUtils.isCompatibleType(viewTypeType, referenceType)) {
                    return Optional.of(violation(
                        "'" + RuleUtils.qualifiedName(leftFeature) + "' is typed '" + viewTypeType.getName()
                            + "' in the view type and '" + referenceType.getName()
                            + "' in the reference, and '" + viewTypeType.getName()
                            + "' is a subtype of '" + referenceType.getName() + "'.",
                        diff
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
