package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import tools.vitruv.neojoin.utils.EMFUtils;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Searches for meta-model files with the {@code .ecore} extension and collects them into a package registry.
 *
 * @see #collect()
 */
public class PackageModelCollector extends AbstractModelCollector {

    public static final String FileExtension = "ecore";

    public PackageModelCollector(String searchPathString) {
        super(searchPathString);
    }

    @Override
    protected Predicate<URI> getFilter() {
        return uri -> Objects.equals(uri.fileExtension(), FileExtension);
    }

    public EPackage.Registry collect() {
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey(FileExtension)) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
                FileExtension, new EcoreResourceFactoryImpl());
        }

        var registry = new EPackageRegistryImpl();

        collectResourcesAsStream(new ResourceSetImpl()).forEach(res -> {
            EMFUtils.getAllEPackages(res).forEach(pack -> {
                var previousValue = registry.put(pack.getNsURI(), pack);
                if (previousValue != null) {
                    throw new IllegalArgumentException("Found multiple packages with URI '%s'.".formatted(pack.getNsURI()));
                }
            });
        });

        return registry;
    }

}
