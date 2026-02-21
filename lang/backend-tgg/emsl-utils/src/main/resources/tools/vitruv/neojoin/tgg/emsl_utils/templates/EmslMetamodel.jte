@import org.eclipse.emf.ecore.EAttribute
@import org.eclipse.emf.ecore.EClass
@import org.eclipse.emf.ecore.EEnum
@import org.eclipse.emf.ecore.EEnumLiteral
@import org.eclipse.emf.ecore.EReference
@import java.util.stream.Collectors
@import static tools.vitruv.neojoin.tgg.emsl_utils.EmslUtils.escapeKeywords
@import static tools.vitruv.neojoin.tgg.emsl_utils.EmslUtils.registerEnum
@param org.eclipse.emf.ecore.EPackage ePackage
@param java.util.Map<String, EEnum> metaModelEnum

!{var eClasses = ePackage.getEClassifiers().stream().filter(EClass.class::isInstance).map(EClass.class::cast).toList();}

metamodel ${escapeKeywords(ePackage.getName())} {
@for(EClass eclass : eClasses)
    !{var superTypeNames = eclass.getESuperTypes().stream().map(EClass::getName).collect(Collectors.joining(", "));}

    ${escapeKeywords(eclass.getName())} @if(!superTypeNames.isEmpty()) : ${superTypeNames} @endif {
    @for(EAttribute attr : eclass.getEAttributes())
        .${escapeKeywords(attr.getName())} : ${escapeKeywords(attr.getEType().getName())}
    @endfor

    @for(EReference ref : eclass.getEReferences())
        !{
            var refLowerBound = ref.getLowerBound();
            String refUpperBound = ref.getUpperBound() == -1 ? "*" : ref.getUpperBound() + "";
            String boundsString = refLowerBound + ".." + refUpperBound;
            boolean isContainment = ref.isContainment();
            String operatorPrefix = isContainment ? "<>" : "";
        }
        ${operatorPrefix}-${escapeKeywords(ref.getName())}(${boundsString})->${escapeKeywords(ref.getEType().getName())}
    @endfor
    }
@endfor


@for(EEnum eenum : ePackage.getEClassifiers().stream().filter(EEnum.class::isInstance).map(EEnum.class::cast).toList())
    enum ${escapeKeywords(registerEnum(metaModelEnum, eenum))} {
    @for(EEnumLiteral literal : eenum.getELiterals())
        ${escapeKeywords(literal.getName())}
    @endfor
    }
@endfor
}
