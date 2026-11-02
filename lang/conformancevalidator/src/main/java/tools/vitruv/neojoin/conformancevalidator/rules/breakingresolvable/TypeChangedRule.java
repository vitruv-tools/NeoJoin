package tools.vitruv.neojoin.conformancevalidator.rules.breakingresolvable;

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
 * Rule 9: CHANGE on eType.
 * The type of a feature differs between view type and reference, and the type of the view
 * type is not a subtype of the type of the reference.
 * Violation, because the view type can hold a value the reference rejects.
 */
public class TypeChangedRule extends ConformanceRule {

    public TypeChangedRule() {
        super("Rule 9", Category.BREAKING_RESOLVABLE);
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
                        && !RuleUtils.isCompatibleType(viewTypeType, referenceType)) {
                    return Optional.of(violation(
                        "'" + RuleUtils.qualifiedName(leftFeature) + "' is typed '" + viewTypeType.getName()
                            + "' in the view type but '" + referenceType.getName()
                            + "' in the reference, and '" + viewTypeType.getName()
                            + "' is not a subtype of '" + referenceType.getName()
                            + "'. Declare the feature with the expected type, as in '"
                            + leftFeature.getName() + " : " + referenceType.getName() + " := ...'.",
                        diff
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
