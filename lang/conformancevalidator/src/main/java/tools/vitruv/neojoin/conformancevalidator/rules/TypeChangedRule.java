package tools.vitruv.neojoin.conformancevalidator.rules;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.ETypedElement;

import tools.vitruv.neojoin.conformancevalidator.Violation;

import java.util.ArrayList;
import java.util.List;

/**
 * R9: CHANGE on eType.
 * Violation because the view type and reference type are incompatible.
 */
public class TypeChangedRule extends ConformanceRule {

    public TypeChangedRule() {
        super("R9", Violation.Category.BREAKING_NOT_RESOLVABLE);
    }

    @Override
    public List<Violation> check(Diff diff) {
        List<Violation> violations = new ArrayList<>();
        if (diff instanceof ReferenceChange rc
                && rc.getReference().getName().equals("eType")
                && diff.getKind() == DifferenceKind.CHANGE) {
            EObject viewtype  = rc.getMatch().getLeft();
            EObject reference = rc.getMatch().getRight();
            if (viewtype instanceof ETypedElement leftFeature && reference instanceof ETypedElement rightFeature) {
                EClassifier viewTypeType  = leftFeature.getEType();
                EClassifier referenceType = rightFeature.getEType();
                if (viewTypeType != null && referenceType != null
                        && !isCompatibleType(viewTypeType, referenceType)) {
                    violations.add(violation(
                        "Type mismatch on '" + leftFeature.getName() +
                        "': view type has '" + viewTypeType.getName() +
                        "' but reference expects '" + referenceType.getName() + "'.",
                        diff
                    ));
                }
            }
        }
        return violations;
    }

    /**
     * R10 helper: checks whether viewTypeClassifier is type-compatible with referenceClassifier.
     * Compatible means: same type, or viewType type is a subtype of the reference type.
     */
    static boolean isCompatibleType(EClassifier viewTypeClassifier, EClassifier referenceClassifier) {
        if (viewTypeClassifier.getName().equals(referenceClassifier.getName())) return true;
        if (viewTypeClassifier instanceof EClass viewClass && referenceClassifier instanceof EClass refClass) {
            return refClass.isSuperTypeOf(viewClass);
        }
        return false;
    }
}
