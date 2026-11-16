package tools.vitruv.neojoin.ide.custom;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.lsp4j.Location;
import org.eclipse.xtext.findReferences.IReferenceFinder;
import org.eclipse.xtext.ide.server.symbol.DocumentSymbolService;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.ast.Join;
import tools.vitruv.neojoin.ast.JoinFeatureCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Finds no definition for elements outside of NeoJoin queries instead of failing the request.
 * <p>
 * Expressions in queries refer to Java types (for example {@code blank} in {@code it.name.blank}) and to the types
 * inferred from the source meta-models (for example {@code name}). Their resources have no language that could load
 * them in the workspace and compute a location: by default, Xtext fails the definition request (and a references
 * request that includes the declaration) with "Cannot create a resource for 'java:/Objects/java.lang.String'".
 * <p>
 * Elements of the source meta-models (classes, imports, declared types, features) have the location of their start tag
 * in the {@code .ecore} file. A feature in a join condition ({@code join Food f with r using name}) belongs to both
 * joined classes and has the location of each.
 * <p>
 * Also returns each location only once: a variable has a JVM parameter in each feature expression, all with the
 * location of the variable (see {@link SourceElementTargetURICollector}).
 */
@Singleton
public class ResolvableDefinitionsDocumentSymbolService extends DocumentSymbolService {

    @Inject
    private EObjectAtOffsetHelper eObjectAtOffsetHelper;

    @Inject
    private IResourceServiceProvider.Registry resourceServiceProviderRegistry;

    @Override
    public List<? extends Location> getDefinitions(
        XtextResource resource,
        int offset,
        IReferenceFinder.IResourceAccess resourceAccess,
        CancelIndicator cancelIndicator
    ) {
        var ecoreLocations = ecoreElementsAt(resource, offset).stream()
            .map(EcoreElementLocator::locate)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (!ecoreLocations.isEmpty()) {
            return ecoreLocations;
        }
        return super.getDefinitions(resource, offset, resourceAccess, cancelIndicator).stream().distinct().toList();
    }

    private List<ENamedElement> ecoreElementsAt(XtextResource resource, int offset) {
        var target = eObjectAtOffsetHelper.getElementWithNameAt(resource, offset);
        var element = target == null ? null : SourceModelElements.ecoreElementOf(target);
        if (element == null) {
            return List.of();
        }

        var elements = new ArrayList<ENamedElement>();
        elements.add(element);
        if (element instanceof EStructuralFeature feature) {
            var otherFeature = featureOfRightClassInJoinCondition(resource, offset, feature);
            if (otherFeature != null) {
                elements.add(otherFeature);
            }
        }
        return elements;
    }

    /**
     * The condition resolves its features in the left class; a join on a feature needs the feature in the right class
     * as well (same name, see {@code NeoJoinScopeProvider}).
     */
    private @Nullable EStructuralFeature featureOfRightClassInJoinCondition(
        XtextResource resource,
        int offset,
        EStructuralFeature feature
    ) {
        var context = eObjectAtOffsetHelper.resolveContainedElementAt(resource, offset);
        if (!(context instanceof JoinFeatureCondition condition)) {
            return null;
        }
        // the class is missing or unresolved while the query is incomplete
        var from = ((Join) condition.eContainer()).getFrom();
        var rightClass = from == null ? null : from.getClazz();
        return rightClass == null || rightClass.eIsProxy() ? null : rightClass.getEStructuralFeature(feature.getName());
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
