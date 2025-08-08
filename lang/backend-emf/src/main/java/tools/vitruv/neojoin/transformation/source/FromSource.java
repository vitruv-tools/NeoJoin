package tools.vitruv.neojoin.transformation.source;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.neojoin.transformation.InstanceTuple;
import tools.vitruv.neojoin.utils.EMFUtils;

import java.util.stream.Stream;

/**
 * Provides {@link InstanceTuple instance tuples} for all objects of the given {@link EClass class} in the given {@link Resource resource}.
 */
public class FromSource implements InstanceSource {

    private final EClass clazz;
    private final Resource resource;

    public FromSource(EClass clazz, Resource resource) {
        this.clazz = clazz;
        this.resource = resource;
    }

    @Override
    public Stream<InstanceTuple> get() {
        return getEObjects().map(InstanceTuple::new);
    }

    public Stream<EObject> getEObjects() {
        return EMFUtils.getAllInstances(resource, clazz);
    }

}
