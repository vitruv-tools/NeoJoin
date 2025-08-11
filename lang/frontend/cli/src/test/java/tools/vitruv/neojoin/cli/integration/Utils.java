package tools.vitruv.neojoin.cli.integration;

import java.net.URL;
import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

public class Utils {
    
    private Utils() {}

    public static URL getResource(String name) {
        return Utils.class.getClassLoader().getResource(name);
    }

    public static Comparison compareEcoreFiles(Path modelA, Path modelB) {
        ResourceSet resourceSetA = new ResourceSetImpl();
        resourceSetA.getResource(URI.createURI(modelA.toUri().toString()), true);

        ResourceSet resourceSetB = new ResourceSetImpl();
        resourceSetB.getResource(URI.createURI(modelB.toUri().toString()), true);

        IComparisonScope scope = new DefaultComparisonScope(resourceSetA, resourceSetB, null);
        return EMFCompare.builder().build().compare(scope);
    }

}
