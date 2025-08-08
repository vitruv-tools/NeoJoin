package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import tools.vitruv.neojoin.utils.EMFUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Searches for instance-model files with the {@code .xmi} extension and collects them into a map indexed by
 * the package that they instantiate.
 *
 * @see #collect()
 */
public class InstanceModelCollector extends AbstractModelCollector {

    public static final String FileExtension = "xmi";

    private final EPackage.Registry registry;

    public InstanceModelCollector(String searchPathString, EPackage.Registry registry) {
        super(searchPathString);
        this.registry = registry;
    }

    @Override
    protected Predicate<URI> getFilter() {
        return uri -> Objects.equals(uri.fileExtension(), FileExtension);
    }

    public Map<EPackage, Resource> collect() {
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey(FileExtension)) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
                FileExtension, new XMIResourceFactoryImpl());
        }

        var knownPackages = EMFUtils.collectAvailablePackages(registry);

        var resourceSet = new ResourceSetImpl();
        resourceSet.setPackageRegistry(registry);

        var map = new HashMap<EPackage, Resource>();
        collectResourcesAsStream(resourceSet).forEach(res -> {
            if (res.getContents().isEmpty()) {
                return;
            }

            var instancedPackage = res.getContents().getFirst().eClass().getEPackage();
            if (!knownPackages.contains(instancedPackage)) {
                return;
            }

            var previous = map.put(instancedPackage, res);
            if (previous != null) {
                throw new IllegalArgumentException("Found multiple instances for package '%s': %s and %s".formatted(
                    instancedPackage.getName(),
                    previous.getURI(),
                    res.getURI()
                ));
            }

        });
        return map;
    }

}
