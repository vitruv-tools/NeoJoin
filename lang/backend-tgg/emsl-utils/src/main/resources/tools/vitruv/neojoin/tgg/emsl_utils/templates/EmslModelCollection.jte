@import org.eclipse.emf.ecore.EObject
@import org.eclipse.emf.ecore.resource.Resource
@import java.util.Map
@param org.eclipse.emf.ecore.resource.ResourceSet rs
@param Map<EObject, String> objIDs

@for(Resource r : rs.getResources())
    @template.EmslModel(resource = r, objIDs = objIDs)
@endfor
