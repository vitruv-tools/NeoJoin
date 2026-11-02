package tools.vitruv.neojoin.collector;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import tools.vitruv.neojoin.utils.EMFUtils;
import tools.vitruv.neojoin.utils.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Searches for meta-model files with the {@code .ecore} extension and collects them into a package registry.
 *
 * @see #collect()
 */
public class PackageModelCollector extends AbstractModelCollector {

    public static final String FileExtension = "ecore";

    @Override
    protected String fileExtension() {
        return FileExtension;
    }

    public PackageModelCollector(String searchPathString) {
        super(searchPathString);
    }

    public Pair<List<Issue>, EPackage.Registry> collect() {
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey(FileExtension)) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
                FileExtension, new EcoreResourceFactoryImpl());
        }

        var registry = new EPackageRegistryImpl();
        List<Issue> issues = new ArrayList<>();

        collectResourcesAsStream(new ResourceSetImpl()).forEach(res ->
            EMFUtils.getAllEPackages(res).forEach(pack -> {
                if (registry.containsKey(pack.getNsURI())) {
                    issues.add(new Issue.PackageURIDuplication(pack.getNsURI()));
                } else {
                    registry.put(pack.getNsURI(), pack);
                }
            })
        );

        return new Pair<>(issues, registry);
    }

    public sealed interface Issue {

        record PackageURIDuplication(String packageUri) implements Issue {

            @Override
            public String toString() {
                return "Found multiple packages with URI '%s'.".formatted(packageUri);
            }
        }
    }
}
