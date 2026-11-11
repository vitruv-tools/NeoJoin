package tools.vitruv.neojoin.utils;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.common.EMFUtils;
import tools.vitruv.neojoin.common.Pair;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Determines from the AST the implicit target classes of a view type and the candidates for their implicit super
 * classes, like {@link tools.vitruv.neojoin.aqr.AQRBuilder} does when it links them.
 */
public class ImplicitSuperClassFinder {

    private final ExpressionHelper expressionHelper;
    private final Map<EClass, Set<Query>> queriesBySourceClass;
    private final FeatureTypeResolver typeResolver;
    private final Map<EClass, EObject> implicitClasses;

    public ImplicitSuperClassFinder(ViewTypeDefinition viewType, ExpressionHelper expressionHelper) {
        this.expressionHelper = expressionHelper;
        this.queriesBySourceClass = AstUtils.mapSourceClassesToQueries(viewType, expressionHelper);
        this.typeResolver = new FeatureTypeResolver(viewType, queriesBySourceClass, expressionHelper);
        this.implicitClasses = new ImplicitClassCollector(typeResolver, expressionHelper)
            .collectImplicitClasses(viewType);
    }

    /**
     * @return the source classes of the implicit target classes, each with the first AST element that causes it (see
     *     {@link ImplicitClassCollector})
     */
    public Map<EClass, EObject> getImplicitClasses() {
        return Map.copyOf(implicitClasses);
    }

    public FeatureInfoResolver createFeatureInfoResolver() {
        return new FeatureInfoResolver(typeResolver, expressionHelper);
    }

    /**
     * Returns the target classes (name and query, the query is {@code null} for an implicit class) of the nearest
     * level of super classes of the given source class (breadth first) that has any, sorted by name. A target class of
     * a source class is a query that selects exactly this source class (single {@code from}, no joins, no grouping,
     * including subqueries), an implicit target class, or the explicit abstract class that is used instead of an
     * implicit target class (see {@link FeatureTypeResolver#findExplicitAbstractQuery}).
     */
    public List<Pair<String, @Nullable Query>> findCandidates(EClass sourceClass) {
        return EMFUtils.findInNearestSuperClasses(sourceClass, this::findTargetClasses)
            .stream()
            .sorted(Comparator.comparing(Pair::left))
            .toList();
    }

    private List<Pair<String, @Nullable Query>> findTargetClasses(EClass sourceClass) {
        var targetClasses = new ArrayList<Pair<String, @Nullable Query>>();
        queriesBySourceClass.getOrDefault(sourceClass, Set.of()).stream()
            .filter(query -> sourceClass.equals(AstUtils.getSingleSourceClass(query, expressionHelper)))
            .forEach(query -> targetClasses.add(new Pair<>(AstUtils.getTargetName(query, expressionHelper), query)));
        if (implicitClasses.containsKey(sourceClass)) {
            targetClasses.add(new Pair<>(sourceClass.getName(), null));
        }
        var abstractQuery = typeResolver.findExplicitAbstractQuery(sourceClass);
        if (abstractQuery != null) {
            targetClasses.add(new Pair<>(AstUtils.getTargetName(abstractQuery, expressionHelper), abstractQuery));
        }
        return targetClasses;
    }

}
