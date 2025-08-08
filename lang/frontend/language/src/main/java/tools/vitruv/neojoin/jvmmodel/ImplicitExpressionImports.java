package tools.vitruv.neojoin.jvmmodel;

import org.eclipse.xtext.xbase.scoping.batch.ImplicitlyImportedFeatures;
import tools.vitruv.neojoin.NeoJoinRuntimeModule;
import tools.vitruv.neojoin.jvmmodel.extensions.Extensions;

import java.util.List;

/**
 * Defines custom extension methods for Xbase expressions.
 *
 * @see NeoJoinRuntimeModule#bindImplicitlyImportedTypes()
 */
public class ImplicitExpressionImports extends ImplicitlyImportedFeatures {

    @Override
    protected List<Class<?>> getExtensionClasses() {
        var classes = super.getExtensionClasses();
        classes.addAll(Extensions.All);
        return classes;
    }

}
