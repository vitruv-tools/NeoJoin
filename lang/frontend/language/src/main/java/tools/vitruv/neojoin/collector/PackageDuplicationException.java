package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

public final class PackageDuplicationException extends Exception {

    public PackageDuplicationException(EPackage pkg, Resource resource1, Resource resource2) {
        this(pkg.getName(), resource1.getURI(), resource2.getURI());
    }

    public PackageDuplicationException(String packageName, URI resourceUri1, URI resourceUri2) {
        super(
            "Found multiple instances for package '%s': %s and %s".formatted(
                packageName,
                resourceUri1,
                resourceUri2
             )
        );
    }

}
