package tools.vitruv.neojoin.collector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import tools.vitruv.neojoin.utils.EMFUtils;
import tools.vitruv.neojoin.utils.Pair;

import static tools.vitruv.neojoin.utils.Utils.iter;
import static tools.vitruv.neojoin.utils.Utils.toMapFailOnDuplicates;

/**
 * Searches for instance-model files with the {@code .xmi} extension and collects them into a map indexed by
 * the package that they instantiate.
 *
 * @see #collect()
 */
public class InstanceModelCollector extends AbstractModelCollector {

    public static final String FileExtension = "xmi";

    @Override
    protected String fileExtension() {
        return FileExtension;
    }

    private final EPackage.Registry registry;

    public InstanceModelCollector(String searchPathString, EPackage.Registry registry) {
        super(searchPathString);
        this.registry = registry;
    }

    public Map<EPackage, Resource> collect() throws PackageDuplicationException, IOException {
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey(FileExtension)) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
                FileExtension, new XMIResourceFactoryImpl());
        }

        var knownPackages = EMFUtils.collectAvailablePackages(registry);

        var resourceSet = new ResourceSetImpl();
        resourceSet.setPackageRegistry(registry);

        var map = new HashMap<EPackage, Resource>();
        for (var resOrIOException : iter(collectResourcesAsStream(resourceSet))) {
            var res = resOrIOException.valueUnsafe();
            var packages = instancedPackageOrNone(res).stream()
                .filter(knownPackages::contains);

            for (var pkg : iter(packages)) {
                var existing = map.putIfAbsent(pkg, res);

                if (existing != null) {
                    throw new PackageDuplicationException(pkg, existing, res);
                }
            }
        }

        return map;
    }

    private static Optional<EPackage> instancedPackageOrNone(Resource res) {
        return res.getContents().isEmpty()? Optional.empty()
            : Optional.of(res.getContents().get(0).eClass().getEPackage());
    }
}
