package tools.vitruv.neojoin;

import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.aqr.AQRTargetClass;

import java.util.List;

/**
 * Base class for tests with the kennel meta-model: the abstract class {@code Animal} with the sub classes {@code Dog}
 * (with the sub class {@code Puppy}) and {@code Cat}, the containers {@code Kennel} (references to animals) and
 * {@code Park}, and {@code Vet} with the enum {@code Specialty}.
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

    protected static AQRTargetClass find(AQR aqr, String name) {
        return aqr.classes().stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

}
