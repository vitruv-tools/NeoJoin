package tools.vitruv.neojoin;

import com.google.inject.Injector;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.StringInputStream;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.aqr.AQRBuilder;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.Pair;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

@SuppressWarnings("NotNullFieldNotInitialized")
public abstract class AbstractIntegrationTest implements HasPackageRegistry {

    private EPackage.Registry packageRegistry;
    private Injector injector;
    private ResourceSet resourceSet;

    @Override
    public EPackage.Registry getPackageRegistry() {
        return packageRegistry;
    }

    public Injector getInjector() {
        return injector;
    }

    @BeforeAll
    protected static void initAIT() {
        if (!EPackage.Registry.INSTANCE.containsKey("http://vitruv.tools/dsls/neojoin/Ast")) {
            EPackage.Registry.INSTANCE.put("http://vitruv.tools/dsls/neojoin/Ast", AstPackage.eINSTANCE);
        }

        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("ecore")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
                "ecore", new EcoreResourceFactoryImpl());
        }
    }

    @BeforeEach
    protected void setUpAIT() throws IOException {
        packageRegistry = createPackageRegistry();
        injector = new NeoJoinStandaloneSetup(packageRegistry).getInjector();

        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
            "nj",
            injector.getInstance(IResourceFactory.class)
        );
    }

    protected EPackage.Registry createPackageRegistry() throws IOException {
        var registry = new EPackageRegistryImpl();
        var packageResourceSet = new ResourceSetImpl();
        for (var path : getMetaModelPaths()) {
            var resource = packageResourceSet.createResource(URI.createURI(path));
            var input = Objects.requireNonNull(getClass().getResourceAsStream(path));
            resource.load(input, null);
            var pack = (EPackage) resource.getContents().getFirst();
            registry.put(pack.getNsURI(), pack);
        }
        return registry;
    }

    protected abstract List<String> getMetaModelPaths();

    protected Pair<ViewTypeDefinition, List<Issue>> internalParse(String query) {
        var resource = resourceSet.createResource(URI.createURI("test.nj"));
        assertThatCode(
            () -> resource.load(new StringInputStream(query), null)
        ).doesNotThrowAnyException();

        // validate parser result (references + custom validations)
        var issues = injector.getInstance(IResourceValidator.class).validate(
            resource,
            CheckMode.ALL,
            CancelIndicator.NullImpl
        );

        var root = resource.getContents().getFirst();
        assertThat(root).isInstanceOf(ViewTypeDefinition.class);

        return new Pair<>((ViewTypeDefinition) root, issues);
    }

    protected AQR internalParseAQR(String query) {
        var result = internalParse(query);
        assertThat(result).hasNoErrors();
        //noinspection DataFlowIssue - false positive
        return new AQRBuilder(result.left(), injector.getInstance(ExpressionHelper.class)).build();
    }

}
