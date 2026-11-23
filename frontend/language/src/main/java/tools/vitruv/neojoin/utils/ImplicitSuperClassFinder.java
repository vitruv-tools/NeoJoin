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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
        return Collections.unmodifiableMap(new LinkedHashMap<>(implicitClasses));
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
        return toCandidates(findNearestTargetClasses(sourceClass));
    }

    /**
     * Finds the source class of the implicit class to which the implicit class of the given source class is linked as
     * implicit sub class: the nearest source super class with an implicit class, if it is the only target class of the
     * nearest level (see {@link #findCandidates}). An implicit candidate always matches the features (see
     * {@code ImplicitSuperClassValidator}), so the link happens.
     *
     * @return the source class of the implicit super class, {@code null} if the implicit class has none
     */
    public @Nullable EClass findImplicitSuperSourceClass(EClass sourceClass) {
        var targetClasses = findNearestTargetClasses(sourceClass);
        var candidates = toCandidates(targetClasses);
        if (candidates.size() != 1 || candidates.get(0).right() != null) {
            return null;
        }

        // the only candidate is implicit, so all target classes of the level are implicit classes of that name
        return targetClasses.get(0).sourceClass();
    }

    /**
     * Checks whether the implicit class of the given source class is the implicit class of the other given source
     * class or is linked to it (transitively) as implicit sub class (see {@link #findImplicitSuperSourceClass}).
     *
     * @param sourceClass              the source class whose implicit class is checked, it needs one unless it is the
     *                                 same class as {@code implicitSuperSourceClass}
     * @param implicitSuperSourceClass the source class of the implicit super class
     */
    public boolean isSameOrImplicitSubClass(EClass sourceClass, EClass implicitSuperSourceClass) {
        @Nullable EClass current = sourceClass;
        while (current != null) {
            if (current == implicitSuperSourceClass) {
                return true;
            }
            if (!implicitClasses.containsKey(current)) {
                return false;
            }
            current = findImplicitSuperSourceClass(current);
        }
        return false;
    }

    private List<TargetClass> findNearestTargetClasses(EClass sourceClass) {
        return EMFUtils.findInNearestSuperClasses(sourceClass, this::findTargetClasses);
    }

    private List<Pair<String, @Nullable Query>> toCandidates(List<TargetClass> targetClasses) {
        return targetClasses.stream()
            .map(targetClass -> new Pair<>(targetClass.name(), targetClass.query()))
            .distinct()
            .sorted(Comparator.comparing(Pair::left))
            .toList();
    }

    private List<TargetClass> findTargetClasses(EClass sourceClass) {
        var targetClasses = new ArrayList<TargetClass>();
        queriesBySourceClass.getOrDefault(sourceClass, Set.of()).stream()
            .filter(query -> sourceClass.equals(AstUtils.getSingleSourceClass(query, expressionHelper)))
            .forEach(query -> targetClasses.add(
                new TargetClass(sourceClass, AstUtils.getTargetName(query, expressionHelper), query)
            ));
        if (implicitClasses.containsKey(sourceClass)) {
            targetClasses.add(new TargetClass(sourceClass, sourceClass.getName(), null));
        }
        var abstractQuery = typeResolver.findExplicitAbstractQuery(sourceClass);
        if (abstractQuery != null) {
            targetClasses.add(
                new TargetClass(sourceClass, AstUtils.getTargetName(abstractQuery, expressionHelper), abstractQuery)
            );
        }
        return targetClasses;
    }

    /**
     * @param sourceClass the source class that the target class belongs to
     * @param query       the query of the target class, {@code null} for an implicit class
     */
    private record TargetClass(EClass sourceClass, String name, @Nullable Query query) {}

}
