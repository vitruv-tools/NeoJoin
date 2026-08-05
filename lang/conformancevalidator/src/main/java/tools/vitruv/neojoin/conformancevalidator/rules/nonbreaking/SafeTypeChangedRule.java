package tools.vitruv.neojoin.conformancevalidator.rules.nonbreaking;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.ETypedElement;

import tools.vitruv.neojoin.conformancevalidator.Violation;
import tools.vitruv.neojoin.conformancevalidator.rules.ConformanceRule;
import tools.vitruv.neojoin.conformancevalidator.rules.breakingnotresolvable.TypeChangedRule;

import java.util.ArrayList;
import java.util.List;

/**
 * R10: CHANGE on eType.
 * The view type's type differs from the reference's type, but it is a compatible (sub)type.
 */
public class SafeTypeChangedRule extends ConformanceRule {

    public SafeTypeChangedRule() {
        super("R10", Violation.Category.NON_BREAKING);
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
                        && TypeChangedRule.isCompatibleType(viewTypeType, referenceType)) {
                    violations.add(violation(
                        "Type of '" + leftFeature.getName() +
                        "' changed from '" + referenceType.getName() +
                        "' to the compatible subtype '" + viewTypeType.getName() + "'.",
                        diff
                    ));
                }
            }
        }
        return violations;
    }
}
