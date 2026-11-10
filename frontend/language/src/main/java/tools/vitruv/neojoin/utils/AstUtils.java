package tools.vitruv.neojoin.utils;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.xbase.XExpression;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.aqr.AQRFeatureOptionsBuilder;
import tools.vitruv.neojoin.ast.AbstractBody;
import tools.vitruv.neojoin.ast.AbstractMainQuery;
import tools.vitruv.neojoin.ast.Body;
import tools.vitruv.neojoin.ast.ConcreteBody;
import tools.vitruv.neojoin.ast.ConcreteFeature;
import tools.vitruv.neojoin.ast.ConcreteMainQuery;
import tools.vitruv.neojoin.ast.Feature;
import tools.vitruv.neojoin.ast.From;
import tools.vitruv.neojoin.ast.Import;
import tools.vitruv.neojoin.ast.Join;
import tools.vitruv.neojoin.ast.MainQuery;
import tools.vitruv.neojoin.ast.MultiplicityExpr;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.ast.Source;
import tools.vitruv.neojoin.ast.SubQuery;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.jvmmodel.TypeInfo;
import tools.vitruv.neojoin.jvmmodel.TypeResolutionException;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static tools.vitruv.neojoin.utils.Assertions.fail;

/**
 * Various utilities for classes from the {@link tools.vitruv.neojoin.ast ast package}.
 */
public final class AstUtils {

    public static final String UnknownTargetName = "<unknown>";

    private AstUtils() {}

    /**
     * Returns the name of the target class of the given main query.
     *
     * @return name of the target class or {@link #UnknownTargetName} if unknown
     */
    public static String getTargetName(MainQuery mainQuery) {
        if (mainQuery.getName() != null) {
            return mainQuery.getName();
        }

        if (mainQuery instanceof ConcreteMainQuery concreteMainQuery) {
            if (concreteMainQuery.getSource() != null) {
                var source = concreteMainQuery.getSource().getFrom().getClazz();
                if (source != null && source.getName() != null) {
                    return source.getName();
                }
            }
        }

        return UnknownTargetName;
    }

    /**
     * Returns the name of the target class of the given subquery with the given inferred type.
     */
    public static String getTargetName(SubQuery subQuery, EClass sourceType) {
        return subQuery.getName() != null ? subQuery.getName() : sourceType.getName();
    }

    /**
     * Returns the name of the target class of the given subquery by inferring its source class with the given
     * expression helper.
     *
     * @return name of the target class or {@link #UnknownTargetName} if unknown
     */
    public static String getTargetName(SubQuery subQuery, ExpressionHelper expressionHelper) {
        if (subQuery.getName() != null) {
            return subQuery.getName();
        }

        var subQuerySourceType = inferSubQuerySourceType(subQuery, expressionHelper);
        if (subQuerySourceType != null) {
            return subQuerySourceType.getName();
        }

        return UnknownTargetName;
    }

    /**
     * Returns the name of the target class of the given query.
     *
     * @see #getTargetName(MainQuery)
     * @see #getTargetName(SubQuery, ExpressionHelper)
     */
    public static String getTargetName(Query query, ExpressionHelper expressionHelper) {
        if (query instanceof MainQuery mainQuery) {
            return getTargetName(mainQuery);
        } else {
            return getTargetName((SubQuery) query, expressionHelper);
        }
    }

    /**
     * Try to infer the source class of the given subquery.
     *
     * @return the inferred source class or {@code null}
     */
    public static @Nullable EClass inferSubQuerySourceType(
        SubQuery subQuery,
        ExpressionHelper expressionHelper
    ) {
        if (subQuery.eContainer() instanceof ConcreteFeature feature) {
            try {
                var typeInfo = expressionHelper.inferEType(feature.getExpression());
                if (typeInfo != null && typeInfo.classifier() instanceof EClass clazz) {
                    return clazz;
                }
            } catch (TypeResolutionException e) {
                // ignore: will be handled by type checking
            }
        }

        return null;
    }

    /**
     * Get {@link ViewTypeDefinition} from any contained element or throw if not contained.
     *
     * @param element contained element
     * @return view type that contains the given element
     */
    public static ViewTypeDefinition getViewType(EObject element) {
        EObject root = EcoreUtil2.getRootContainer(element);
        if (root instanceof ViewTypeDefinition vt) {
            return vt;
        }

        return fail("Element %s is not contained in a ViewType".formatted(element.toString()));
    }

    /**
     * Returns the name with which the package from the given import can be referenced within the query.
     */
    public static String getImportAlias(Import imp) {
        return imp.getAlias() != null ? imp.getAlias() : imp.getPackage().getName();
    }

    public static Stream<Import> getValidImports(ViewTypeDefinition viewType) {
        return viewType.getImports().stream()
            // filter out unresolved imports
            .filter(imp -> imp.getPackage() != null && imp.getPackage().getName() != null);
    }

    /**
     * Returns a map from {@link #getImportAlias(Import) import alias} to package.
     */
    public static Map<String, EPackage> getImportedPackagesByAlias(ViewTypeDefinition viewType) {
        return getValidImports(viewType)
            .map(imp -> Map.entry(getImportAlias(imp), imp.getPackage()))
            .collect(Utils.mapCollector(false)); // ignore duplicates -> will be handled in validator
    }

    /**
     * Infer the type of the expression.
     *
     * @return the type or {@code null} if it cannot be inferred (this is handled by type checking)
     */
    public static @Nullable TypeInfo inferTypeOrNull(XExpression expression, ExpressionHelper expressionHelper) {
        try {
            return expressionHelper.inferEType(expression);
        } catch (TypeResolutionException e) {
            return null; // ignore: will be handled by type checking
        }
    }

    /**
     * Get the feature that the expression references, if it references one.
     *
     * @return the feature or {@code null} if the expression does not reference a feature or its type cannot be
     *     resolved (this is handled by type checking)
     */
    public static @Nullable EStructuralFeature getReferencedFeatureOrNull(
        XExpression expression,
        ExpressionHelper expressionHelper
    ) {
        try {
            return expressionHelper.getFeatureOrNull(expression);
        } catch (TypeResolutionException e) {
            return null; // ignore: will be handled by type checking
        }
    }

    /**
     * Get the distinct, resolved super classes of the given query (without the query itself).
     */
    public static List<Query> getResolvedSuperClasses(MainQuery query) {
        var result = new LinkedHashSet<Query>();
        for (var superClass : query.getSuperClasses()) {
            if (!superClass.eIsProxy() && superClass != query) {
                result.add(superClass);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Check whether the given query is the same as one of its super classes or a (transitive) super class of them.
     */
    public static boolean hasCyclicSuperClasses(MainQuery query) {
        return query.getSuperClasses().stream().anyMatch(superClass ->
            !superClass.eIsProxy() && (superClass == query || isSameOrSuperClassOf(query, superClass))
        );
    }

    /**
     * Check whether the given queries are the same or whether the first query defines a (transitive) super class of
     * the type defined by the second query. Terminates for cyclic inheritance.
     *
     * @return {@code true} if {@code superClassQuery} is {@code subClassQuery} or one of its (transitive) super classes
     */
    public static boolean isSameOrSuperClassOf(Query superClassQuery, Query subClassQuery) {
        var visited = new HashSet<Query>();
        var pending = new ArrayDeque<Query>();
        pending.push(subClassQuery);
        while (!pending.isEmpty()) {
            var current = pending.pop();
            if (current == superClassQuery) {
                return true;
            }
            if (visited.add(current) && current instanceof MainQuery mainQuery) {
                for (var superClass : mainQuery.getSuperClasses()) {
                    if (!superClass.eIsProxy()) {
                        pending.push(superClass);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Map each source class to the queries that select it, either in the {@code from} or {@code join} clause of a
     * main query or as the inferred source class of a subquery.
     *
     * @param viewType the view type whose queries are mapped
     * @return queries by source class, source classes that cannot be resolved are skipped
     */
    public static Map<EClass, Set<Query>> mapSourceClassesToQueries(
        ViewTypeDefinition viewType,
        ExpressionHelper expressionHelper
    ) {
        var queriesBySourceClass = new HashMap<EClass, Set<Query>>();
        getAllQueries(viewType).forEach(query -> {
            if (query instanceof ConcreteMainQuery mainQuery) {
                if (mainQuery.getSource() != null) {
                    getAllFroms(mainQuery.getSource())
                        .map(From::getClazz)
                        .filter(clazz -> clazz != null && !clazz.eIsProxy())
                        .forEach(clazz -> queriesBySourceClass
                            .computeIfAbsent(clazz, k -> new HashSet<>())
                            .add(mainQuery));
                }
            } else if (query instanceof SubQuery subQuery) {
                var sourceClass = inferSubQuerySourceType(subQuery, expressionHelper);
                if (sourceClass != null) {
                    queriesBySourceClass.computeIfAbsent(sourceClass, k -> new HashSet<>()).add(subQuery);
                }
            }
        });
        return queriesBySourceClass;
    }

    /**
     * Maps the name of each abstract class (an {@code abstract} query, which has no source) to its query. If several
     * abstract classes have the same name, the first one is used.
     */
    public static Map<String, Query> mapAbstractQueriesByName(
        ViewTypeDefinition viewType,
        ExpressionHelper expressionHelper
    ) {
        var abstractQueries = new HashMap<String, Query>();
        getAllQueries(viewType)
            .filter(AbstractMainQuery.class::isInstance)
            .forEach(query -> abstractQueries.putIfAbsent(getTargetName(query, expressionHelper), query));
        return abstractQueries;
    }

    /**
     * Returns whether the given query can select instances of the given class: one of its source classes is the class
     * or a sub class of it, or a super class of it that is selected including sub classes ({@code from extends}, and
     * always for subqueries).
     */
    public static boolean canSelectInstancesOf(Query query, EClass clazz, ExpressionHelper expressionHelper) {
        if (query instanceof ConcreteMainQuery mainQuery) {
            var source = mainQuery.getSource();
            return source != null && getAllFroms(source).anyMatch(from -> {
                var sourceClass = from.getClazz();
                return sourceClass != null
                    && !sourceClass.eIsProxy()
                    && (clazz.isSuperTypeOf(sourceClass)
                        || (from.isIncludeSubClasses() && sourceClass.isSuperTypeOf(clazz)));
            });
        } else if (query instanceof SubQuery subQuery) {
            var sourceClass = inferSubQuerySourceType(subQuery, expressionHelper);
            return sourceClass != null && (clazz.isSuperTypeOf(sourceClass) || sourceClass.isSuperTypeOf(clazz));
        }
        return false;
    }

    /**
     * Get the source class of a query that selects exactly one source class: a main query with a single {@code from},
     * no joins and no grouping, or a subquery.
     *
     * @return the source class or {@code null} if the query selects differently or the class cannot be determined
     */
    public static @Nullable EClass getSingleSourceClass(Query query, ExpressionHelper expressionHelper) {
        if (query instanceof ConcreteMainQuery mainQuery) {
            var source = mainQuery.getSource();
            if (source != null && source.getJoins().isEmpty() && !isGrouping(source) && source.getFrom() != null) {
                var sourceClass = source.getFrom().getClazz();
                return sourceClass != null && !sourceClass.eIsProxy() ? sourceClass : null;
            }
            return null;
        }
        return getCopiedSourceClass(query, expressionHelper);
    }

    /**
     * Get the source class whose features are copied by a query without body.
     *
     * @return the source class or {@code null} if the query has no source or a source with joins (which is not
     *     allowed without body, but handled elsewhere) or if the source class of a subquery cannot be inferred
     */
    public static @Nullable EClass getCopiedSourceClass(Query query, ExpressionHelper expressionHelper) {
        if (query instanceof ConcreteMainQuery mainQuery) {
            var source = mainQuery.getSource();
            if (source != null && source.getJoins().isEmpty() && source.getFrom() != null) {
                return source.getFrom().getClazz();
            }
        } else if (query instanceof SubQuery subQuery) {
            return inferSubQuerySourceType(subQuery, expressionHelper);
        }
        return null;
    }

    /**
     * Get the name of the feature, either the explicitly given name or the name of the copied feature.
     *
     * @return the name or {@code null} if it cannot be determined (this is handled by other validation rules)
     */
    public static @Nullable String getFeatureNameOrNull(Feature feature, ExpressionHelper expressionHelper) {
        if (feature.getName() != null) {
            return feature.getName();
        }

        if (feature instanceof ConcreteFeature concreteFeature && concreteFeature.getExpression() != null) {
            try {
                var eFeature = expressionHelper.getFeatureOrNull(concreteFeature.getExpression());
                if (eFeature != null) {
                    return eFeature.getName();
                }
            } catch (TypeResolutionException e) {
                // ignore: will be handled by type checking
            }
        }

        return null;
    }

    /**
     * Get all {@link From froms} contained in {@code source} including the one {@link Source#getFrom() directly
     * contained} as well as those contained in {@link Source#getJoins() joins}.
     *
     * @param source the source to get the froms from
     * @return a stream of all froms in the source
     */
    public static Stream<From> getAllFroms(Source source) {
        return Stream.concat(
            Stream.of(source.getFrom()),
            source.getJoins().stream()
                .map(Join::getFrom)
        ).filter(Objects::nonNull); // might be null for incomplete queries
    }

    /**
     * Returns whether the given source contains any grouping expressions. Returns {@code false} if no source was
     * provided.
     */
    public static boolean isGrouping(@Nullable Source source) {
        if (source == null) {
            return false;
        }

        return !source.getGroupingExpressions().isEmpty();
    }

    /**
     * Returns the first {@link MultiplicityExpr multiplicity expression} contained in the given features modifiers or
     * {@code null} if none is found.
     */
    public static @Nullable MultiplicityExpr findMultiplicityExpression(Feature feature) {
        return feature.getModifiers().stream()
            .filter(MultiplicityExpr.class::isInstance)
            .map(MultiplicityExpr.class::cast)
            .findFirst()
            .orElse(null);
    }

    /**
     * Returns whether the given multiplicity accepts more than one element.
     *
     * @param multiplicity pair of lower and upper bound
     */
    public static boolean isManyMultiplicity(Pair<Integer, Integer> multiplicity) {
        //noinspection DataFlowIssue - false positive
        return multiplicity.right() == ETypedElement.UNBOUNDED_MULTIPLICITY || multiplicity.right() > 1;
    }

    /**
     * Returns whether the given multiplicity accepts more than one element.
     */
    public static boolean isManyMultiplicity(MultiplicityExpr multiplicity) {
        return isManyMultiplicity(AQRFeatureOptionsBuilder.normalizeMultiplicity(multiplicity));
    }

    /**
     * Whether the multiplicity that is written for the feature is many. A feature without multiplicity is single.
     */
    public static boolean isManyMultiplicityDefinition(Feature feature) {
        var explicitMultiplicity = findMultiplicityExpression(feature);
        if (explicitMultiplicity != null) {
            return isManyMultiplicity(explicitMultiplicity);
        } else {
            return false; // should we try to infer this from the declared type?
        }
    }

    /**
     * Get all queries (main and sub queries) contained in the given view type.
     */
    public static Stream<Query> getAllQueries(ViewTypeDefinition viewType) {
        return viewType.getQueries().stream()
            .flatMap(AstUtils::getAllQueriesImpl);
    }

    private static Stream<Query> getAllQueriesImpl(Query query) {
        var body = getBody(query);
        if (body == null) {
            return Stream.of(query);
        } else if (body instanceof ConcreteBody concreteBody) {
            var subQueries = concreteBody.getFeatures().stream()
                .map(ConcreteFeature::getSubQuery)
                .filter(Objects::nonNull)
                .flatMap(AstUtils::getAllQueriesImpl);
            return Stream.concat(Stream.of(query), subQueries);
        } else if (body instanceof AbstractBody) {
            // abstract features cannot have subqueries
            return Stream.of(query);
        } else {
            return fail("A query body must be either an abstract or a concrete body");
        }
    }

    public static @Nullable Body getBody(Query query) {
        if (query instanceof ConcreteMainQuery concreteMainQuery) {
            return concreteMainQuery.getBody();
        } else if (query instanceof AbstractMainQuery abstractMainQuery) {
            return abstractMainQuery.getBody();
        } else if (query instanceof SubQuery subQuery) {
            return subQuery.getBody();
        } else {
            return fail("Query must be either a concrete main query, an abstract main query, or a subquery");
        }
    }

    public static List<Feature> getFeatures(Body body) {
        if (body instanceof ConcreteBody concreteBody) {
            return List.copyOf(concreteBody.getFeatures());
        } else if (body instanceof AbstractBody abstractBody) {
            return List.copyOf(abstractBody.getFeatures());
        } else {
            return fail("Body must be either a concrete query body or an abstract query body");
        }
    }

}
