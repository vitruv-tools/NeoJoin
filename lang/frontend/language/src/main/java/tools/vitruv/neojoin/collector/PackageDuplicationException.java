package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;

public final class PackageDuplicationException extends Exception {

    public PackageDuplicationException(String packageName, URI packageURI1, URI packageURI2) {
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
