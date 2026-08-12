package tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.ETypedElement;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;

import java.util.Optional;

/**
 * R9: CHANGE on eType.
 * The type of a feature differs between view type and reference.
 * Violation because the view type and reference type are incompatible.
 */
public class TypeChangedRule extends ConformanceRule {

    public TypeChangedRule() {
        super("R9", Violation.Category.BREAKING_NOT_RESOLVABLE);
    }

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
                        && !isCompatibleType(viewTypeType, referenceType)) {
                    return Optional.of(violation(
                        "Type mismatch on '" + leftFeature.getName() +
                        "': view type has '" + viewTypeType.getName() +
                        "' but reference expects '" + referenceType.getName() + "'.",
                        diff
                    ));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks whether viewTypeClassifier is type-compatible with referenceClassifier.
     */
    public static boolean isCompatibleType(EClassifier viewTypeClassifier, EClassifier referenceClassifier) {
        if (viewTypeClassifier.getName().equals(referenceClassifier.getName())) return true;
        if (viewTypeClassifier instanceof EClass viewClass && referenceClassifier instanceof EClass refClass) {
            return refClass.isSuperTypeOf(viewClass);
        }
        return false;
    }
}
