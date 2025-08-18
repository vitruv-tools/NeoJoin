package tools.vitruv.neojoin.cli.integration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import static tools.vitruv.neojoin.utils.Assertions.check;

public class Utils {

    private Utils() {}

    public static final Path INSTANCES = Path.of("instances");
    public static final Path MODELS = Path.of("models");
    public static final Path QUERIES = Path.of("queries");
    public static final Path RESULTS = Path.of("results");

    public static Path getResource(Path path) {
        try {
            var url = Utils.class.getClassLoader().getResource(path.toString());
            check(url != null);
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static EPackage.Registry createPackageRegistry(List<Path> metaModelPaths) throws IOException {
        EPackageRegistryImpl registry = new EPackageRegistryImpl();
        ResourceSet packageResourceSet = new ResourceSetImpl();

        for (Path path : metaModelPaths) {
            Resource resource = packageResourceSet.getResource(org.eclipse.emf.common.util.URI.createURI(path.toUri().toString()), true);
            EPackage pack = (EPackage) resource.getContents().getFirst();

            registry.put(pack.getNsURI(), pack);
        }

        return registry;
    }

    public static Comparison compareEcoreFiles(Path modelA, Path modelB) {
        ResourceSet resourceSetA = new ResourceSetImpl();
        resourceSetA.getResource(org.eclipse.emf.common.util.URI.createURI(modelA.toUri().toString()), true);

        ResourceSet resourceSetB = new ResourceSetImpl();
        resourceSetB.getResource(org.eclipse.emf.common.util.URI.createURI(modelB.toUri().toString()), true);

        IComparisonScope scope = new DefaultComparisonScope(resourceSetA, resourceSetB, null);
        return EMFCompare.builder().build().compare(scope);
    }

    public static Comparison compareInstanceFiles(Path model, Path instanceA, Path instanceB) throws IOException {
        EPackage.Registry packageRegistry = createPackageRegistry(List.of(model));

        ResourceSet resourceSetA = new ResourceSetImpl();
        resourceSetA.setPackageRegistry(packageRegistry);
        resourceSetA.getResource(org.eclipse.emf.common.util.URI.createURI(instanceA.toUri().toString()), true);

        ResourceSet resourceSetB = new ResourceSetImpl();
        resourceSetB.setPackageRegistry(packageRegistry);
        resourceSetB.getResource(org.eclipse.emf.common.util.URI.createURI(instanceB.toUri().toString()), true);

        IComparisonScope scope = new DefaultComparisonScope(resourceSetA, resourceSetB, null);
        return EMFCompare.builder().build().compare(scope);
    }

}
