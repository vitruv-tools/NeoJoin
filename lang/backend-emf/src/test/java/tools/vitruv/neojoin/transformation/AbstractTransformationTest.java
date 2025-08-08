package tools.vitruv.neojoin.transformation;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import tools.vitruv.neojoin.AbstractIntegrationTest;
import tools.vitruv.neojoin.generation.MetaModelGenerator;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.EMFUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tools.vitruv.neojoin.utils.Assertions.check;

@SuppressWarnings("NotNullFieldNotInitialized")
public abstract class AbstractTransformationTest extends AbstractIntegrationTest {

    private Map<EPackage, Resource> instanceModelRegistry;

    @BeforeAll
    protected static void initATT() {
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("xmi")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
                "xmi", new XMIResourceFactoryImpl());
        }
    }

    @BeforeEach
    protected void setUpATT() throws IOException {
        instanceModelRegistry = createInstanceModelRegistry();
    }

    protected Map<EPackage, Resource> createInstanceModelRegistry() throws IOException {
        var knownPackages = EMFUtils.collectAvailablePackages(getPackageRegistry());

        var modelResourceSet = new ResourceSetImpl();
        modelResourceSet.setPackageRegistry(getPackageRegistry());

        var registry = new HashMap<EPackage, Resource>();
        for (var path : getInstanceModelPaths()) {
            var resource = modelResourceSet.createResource(URI.createURI(path));
            var input = Objects.requireNonNull(getClass().getResourceAsStream(path));
            resource.load(input, null);

            check(!resource.getContents().isEmpty());
            var instancedPackage = resource.getContents().getFirst().eClass().getEPackage();
            check(knownPackages.contains(instancedPackage));
            var previous = registry.put(instancedPackage, resource);
            check(previous == null);
        }
        return registry;
    }

    protected abstract List<String> getInstanceModelPaths();

    protected EObject internalTransform(String query) {
        var aqr = internalParseAQR(query);
        var targetMetaModel = new MetaModelGenerator(aqr).generate().pack();
        return new Transformator(
            getInjector().getInstance(ExpressionHelper.class),
            aqr,
            targetMetaModel,
            instanceModelRegistry
        ).transform();
    }

}
