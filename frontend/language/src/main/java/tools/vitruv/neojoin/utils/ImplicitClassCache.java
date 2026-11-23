package tools.vitruv.neojoin.utils;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Tuples;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;

import java.util.Map;

/**
 * Caches the {@link ImplicitSuperClassFinder} (and with it the implicit classes) of a view type per resource.
 * <p>
 * Determining the implicit classes infers the types of all features. The scope of every {@code Feature.type} link and
 * several validation checks need them, so without the cache the work is repeated per feature and check (quadratic in
 * the number of features).
 * <p>
 * The implicit classes only depend on the contents of the resource and on the meta-models, so the cache is valid as
 * long as the resource is unchanged. The {@code IResourceScopeCache} of Xtext ({@code OnChangeEvictingCache}) clears
 * the entries of a resource on every change of its contents. Lazy linking resolves proxies, which EMF reports as touch
 * notifications that do not clear the cache; installing or discarding the derived JVM model clears it.
 */
public class ImplicitClassCache {

    @Inject
    private IResourceScopeCache cache;

    @Inject
    private ExpressionHelper expressionHelper;

    /**
     * @return the finder of the view type, computed once per resource state
     */
    public ImplicitSuperClassFinder getFinder(ViewTypeDefinition viewType) {
        return cache.get(
            Tuples.pair(ImplicitClassCache.class, viewType),
            viewType.eResource(),
            () -> new ImplicitSuperClassFinder(viewType, expressionHelper)
        );
    }

    /**
     * @return the source classes of the implicit classes of the view type in collector order, each with the first AST
     *     element that causes it (see {@link ImplicitClassCollector})
     */
    public Map<EClass, EObject> getImplicitClasses(ViewTypeDefinition viewType) {
        return getFinder(viewType).getImplicitClasses();
    }

}
