package tools.vitruv.neojoin;

import tools.vitruv.neojoin.aqr.AQR;

import java.util.List;

/**
 * Base class for tests with the kennel meta-model (class hierarchy {@code Animal <- Dog <- Puppy}, enum {@code Specialty}).
 */
public abstract class AbstractKennelTest extends AbstractIntegrationTest implements PackageRegistryUtils {

    @Override
    protected List<String> getMetaModelPaths() {
        return List.of("/models/kennel.ecore");
    }

    protected AQR parse(String query) {
        return internalParseAQR("""
            export view to "http://example.com/view"
            
            import "http://example.org/kennel"
            """ + query);
    }

}
