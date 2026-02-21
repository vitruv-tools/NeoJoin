@import org.eclipse.emf.common.util.EList
@import org.eclipse.emf.ecore.EAttribute
@import org.eclipse.emf.ecore.EObject
@import org.eclipse.emf.ecore.EReference
@import java.util.ArrayList
@import java.util.List
@import org.eclipse.emf.ecore.EPackage
@import static tools.vitruv.neojoin.tgg.emsl_utils.EmslUtils.escapeKeywords
@import static tools.vitruv.neojoin.tgg.emsl_utils.EmslUtils.extractModelName
@import static tools.vitruv.neojoin.tgg.emsl_utils.EmslUtils.getBuiltInValue
@param org.eclipse.emf.ecore.resource.Resource resource
@param java.util.Map<EObject, String> objIDs

!{var contents = resource.getContents().stream().filter(content -> !(content instanceof  EPackage)).toList();}
@for(EObject rootOfModel : contents)
    model ${extractModelName(resource.getURI())} {

    !{
        List<EObject> eAllContents = new ArrayList<>();
        eAllContents.add(rootOfModel);
        var it = rootOfModel.eAllContents();
        while (it.hasNext()) {
            eAllContents.add(it.next());
        }
    }

    @for(EObject modelElt : eAllContents)
        ${objIDs.get(modelElt)} : ${escapeKeywords(modelElt.eClass().getName())} {
        @for (EAttribute attr : modelElt.eClass().getEAllAttributes())
            @if (modelElt.eGet(attr) != null)
                .${escapeKeywords(attr.getName())} : ${getBuiltInValue(attr, modelElt)}
            @endif
        @endfor

        @for (EReference ref : modelElt.eClass().getEAllReferences())
            !{ Object refVal = modelElt.eGet(ref); }
            @if (refVal instanceof EList<?>)
                @for (EObject reference : (EList<EObject>) refVal)
                    -${escapeKeywords(ref.getName())} -> ${objIDs.get(reference)}
                @endfor
            @elseif (refVal != null)
                -${escapeKeywords(ref.getName())} -> ${objIDs.get((EObject) refVal)}
            @endif
        @endfor
        }
    @endfor
    }
@endfor
