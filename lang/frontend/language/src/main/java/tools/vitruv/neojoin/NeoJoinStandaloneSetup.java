package tools.vitruv.neojoin;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;

/**
 * Standalone NeoJoin language initialization.
 */
public class NeoJoinStandaloneSetup extends NeoJoinStandaloneSetupGenerated {

    private final EPackage.Registry packageRegistry;
    private final Injector injector;

    public NeoJoinStandaloneSetup(EPackage.Registry packageRegistry) {
        this.packageRegistry = packageRegistry;
        this.injector = createInjectorAndDoEMFRegistration();
    }

    protected Module createModule() {
        return new NeoJoinRuntimeModule.Dynamic(packageRegistry);
    }

    @Override
    @Deprecated // dont use this method directly
    public Injector createInjector() {
        return Guice.createInjector(createModule());
    }

    public Injector getInjector() {
        return injector;
    }

    public Parser getParser() {
        return injector.getInstance(Parser.class);
    }

    public ExpressionHelper getExpressionHelper() {
        return injector.getInstance(ExpressionHelper.class);
    }

}
