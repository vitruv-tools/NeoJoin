package tools.vitruv.neojoin.ide.custom;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.lsp4j.Location;
import org.eclipse.xtext.findReferences.IReferenceFinder;
import org.eclipse.xtext.ide.server.symbol.DocumentSymbolService;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

import java.util.List;

/**
 * Finds no definition for elements outside of NeoJoin queries instead of failing the request.
 * <p>
 * Expressions in queries refer to Java types (for example {@code blank} in {@code it.name.blank}) and to the types
 * inferred from the source meta-models (for example {@code name}). Their resources have no language that could load
 * them in the workspace and compute a location: by default, Xtext fails the definition request (and a references
 * request that includes the declaration) with "Cannot create a resource for 'java:/Objects/java.lang.String'".
 * <p>
 * Also returns each location only once: a variable has a JVM parameter in each feature expression, all with the
 * location of the variable (see {@link SourceElementTargetURICollector}).
 */
@Singleton
public class ResolvableDefinitionsDocumentSymbolService extends DocumentSymbolService {

    @Inject
    private IResourceServiceProvider.Registry resourceServiceProviderRegistry;

    @Override
    public List<? extends Location> getDefinitions(
        XtextResource resource,
        int offset,
        IReferenceFinder.IResourceAccess resourceAccess,
        CancelIndicator cancelIndicator
    ) {
        return super.getDefinitions(resource, offset, resourceAccess, cancelIndicator).stream().distinct().toList();
    }

    @Override
    protected void doRead(
        IReferenceFinder.IResourceAccess resourceAccess,
        URI objectURI,
        Procedure1<? super EObject> acceptor
    ) {
        // the location of a definition needs the services of its language (see getDefinitions)
        if (resourceServiceProviderRegistry.getResourceServiceProvider(objectURI.trimFragment()) != null) {
            super.doRead(resourceAccess, objectURI, acceptor);
        }
    }

}
