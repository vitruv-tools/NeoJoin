package tools.vitruv.neojoin.transformation.source;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.neojoin.transformation.InstanceTuple;
import tools.vitruv.neojoin.utils.EMFUtils;

import java.util.stream.Stream;

/**
 * Provides {@link InstanceTuple instance tuples} for the objects of the given {@link EClass class} in the given
 * {@link Resource resource}: the direct instances of the class and, if {@code includeSubClasses} is set, also the
 * instances of its sub classes.
 */
public class FromSource implements InstanceSource {

    private final EClass clazz;
    private final Resource resource;
    private final boolean includeSubClasses;

    public FromSource(EClass clazz, Resource resource, boolean includeSubClasses) {
        this.clazz = clazz;
        this.resource = resource;
        this.includeSubClasses = includeSubClasses;
    }

    @Override
    public Stream<InstanceTuple> get() {
        return getEObjects().map(InstanceTuple::new);
    }

    public Stream<EObject> getEObjects() {
        if (includeSubClasses) {
            return EMFUtils.getAllInstances(resource, clazz);
        } else {
            return EMFUtils.getAllDirectInstances(resource, clazz);
        }
    }

}
