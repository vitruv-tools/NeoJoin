package tools.vitruv.neojoin.transformation.emf;

import com.google.inject.Injector;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.util.StringInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import tools.vitruv.neojoin.NeoJoinStandaloneSetup;
import tools.vitruv.neojoin.Parser;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.common.EMFUtils;
import tools.vitruv.neojoin.generation.MetaModelGenerator;
import tools.vitruv.neojoin.model.AQR;
import tools.vitruv.neojoin.model.ExpressionInterpreter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tools.vitruv.neojoin.common.Assertions.check;

@SuppressWarnings("NotNullFieldNotInitialized")
public abstract class AbstractTransformationTest {

    private EPackage.Registry packageRegistry;
    private Injector injector;
    private ResourceSet resourceSet;
    private Map<EPackage, Resource> instanceModelRegistry;

    public EPackage.Registry getPackageRegistry() {
        return packageRegistry;
    }

    public Injector getInjector() {
        return injector;
    }

    @BeforeAll
    protected static void initATT() {
        if (!EPackage.Registry.INSTANCE.containsKey("http://vitruv.tools/dsls/neojoin/Ast")) {
            EPackage.Registry.INSTANCE.put("http://vitruv.tools/dsls/neojoin/Ast", AstPackage.eINSTANCE);
        }

        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("ecore")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
                "ecore", new EcoreResourceFactoryImpl());
        }

        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("xmi")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
                "xmi", new XMIResourceFactoryImpl());
        }
    }

    @BeforeEach
    protected void setUpATT() throws IOException {
        packageRegistry = createPackageRegistry();
        // Xtext keeps the first validator registered per JVM, which would use the package registry of the first test
        // class. Dropping it lets each setup register a validator bound to its own registry.
        EValidator.Registry.INSTANCE.remove(AstPackage.eINSTANCE);
        injector = new NeoJoinStandaloneSetup(packageRegistry).getInjector();

        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
            "nj",
            injector.getInstance(IResourceFactory.class)
        );

        instanceModelRegistry = createInstanceModelRegistry();
    }

    protected EPackage.Registry createPackageRegistry() throws IOException {
        var registry = new EPackageRegistryImpl();
        var packageResourceSet = new ResourceSetImpl();
        for (var path : getMetaModelPaths()) {
            var resource = packageResourceSet.createResource(URI.createURI(path));
            var input = Objects.requireNonNull(getClass().getResourceAsStream(path));
            resource.load(input, null);
            var pack = (EPackage) resource.getContents().get(0);
            registry.put(pack.getNsURI(), pack);
        }
        return registry;
    }

    protected abstract List<String> getMetaModelPaths();

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

    private AQR internalParseAQR(String query) {
        var resource = resourceSet.createResource(URI.createURI("test.nj"));
        try {
            resource.load(new StringInputStream(query), null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // validates the parser result (references + custom validations) and builds the AQR
        var result = injector.getInstance(Parser.class).parse(resource);
        if (result instanceof Parser.Result.Success success) {
            return success.aqr();
        }
        throw new AssertionError("Query has errors: " + result.issues());
    }

    protected EObject internalTransform(String query) {
        var aqr = internalParseAQR(query);
        var targetMetaModel = new MetaModelGenerator(aqr).generate().pack();
        return new Transformator(
            getInjector().getInstance(ExpressionInterpreter.class),
            aqr,
            targetMetaModel,
            instanceModelRegistry,
            Map.of()
        ).transform();
    }

}
