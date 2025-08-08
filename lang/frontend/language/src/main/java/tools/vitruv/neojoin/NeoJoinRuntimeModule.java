package tools.vitruv.neojoin;

import com.google.inject.Binder;
import com.google.inject.name.Names;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.scoping.batch.ImplicitlyImportedFeatures;
import tools.vitruv.neojoin.jvmmodel.ImplicitExpressionImports;

/**
 * Xtext service registry, see <a href="https://eclipse.dev/Xtext/documentation/302_configuration.html#dependency-injection">Xtext documentation</a>.
 */
@SuppressWarnings("unused")
public class NeoJoinRuntimeModule extends AbstractNeoJoinRuntimeModule {

    public Class<? extends ImplicitlyImportedFeatures> bindImplicitlyImportedTypes() {
        return ImplicitExpressionImports.class;
    }

    public static class Dynamic extends NeoJoinRuntimeModule {

        private final EPackage.Registry registry;

        public Dynamic(EPackage.Registry registry) {
            this.registry = registry;
        }

        @Override
        public void configure(Binder binder) {
            super.configure(binder);
            binder.bind(EPackage.Registry.class)
                .annotatedWith(Names.named(Constants.ImportPackageRegistry))
                .toInstance(registry);
        }

    }

}
