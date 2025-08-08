package tools.vitruv.neojoin.aqr;

import tools.vitruv.neojoin.AbstractIntegrationTest;
import tools.vitruv.neojoin.PackageRegistryUtils;

import java.util.List;

public class AbstractAQRTest extends AbstractIntegrationTest implements PackageRegistryUtils {

    @Override
    protected List<String> getMetaModelPaths() {
        return List.of("/models/restaurant.ecore", "/models/reviewpage.ecore");
    }

    protected AQR parse(String query) {
        return internalParseAQR("""
            export package to "http://example.com"
            
            import "http://example.org/restaurant" as rest
            import "http://example.org/reviewpage"
            
            """ + query);
    }

}
