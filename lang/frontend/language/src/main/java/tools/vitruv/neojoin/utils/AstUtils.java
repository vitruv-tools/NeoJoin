package tools.vitruv.neojoin.utils;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.xtext.EcoreUtil2;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.aqr.AQRFeatureOptionsBuilder;
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
import tools.vitruv.neojoin.jvmmodel.TypeResolutionException;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static tools.vitruv.neojoin.utils.Assertions.fail;

/**
 * Various utilities for classes from the {@link tools.vitruv.neojoin.ast ast package}.
 */
public final class AstUtils {

    public static final String UnknownTargetName = "<unknown>";

    /**
     * Returns the name of the target class of the given main query.
     *
     * @return name of the target class or {@link #UnknownTargetName} if unknown
     */
    public static String getTargetName(MainQuery mainQuery) {
        if (mainQuery.getName() != null) {
            return mainQuery.getName();
        }

        if (mainQuery.getSource() != null) {
            var source = mainQuery.getSource().getFrom().getClazz();
            if (source != null && source.getName() != null) {
                return source.getName();
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
     * Returns the name of the target class of the given subquery by inferring its source class with the given expression helper.
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
        if (subQuery.eContainer() instanceof Feature feature) {
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
     * Check whether the given inferred class can be assigned to a feature with a query type with the given source.
     *
     * @param sourceClazz source class of the query referenced by the explicit type
     * @param inferred    inferred class
     * @return {@code true} if the assignment is valid, {@code false} otherwise
     */
    public static boolean checkSourceType(EClass sourceClazz, EClass inferred) {
        return sourceClazz.isSuperTypeOf(inferred);
    }

    /**
     * Check whether the given inferred class can be assigned to a feature with an explicit query type with the given source.
     *
     * @param source   source of the query referenced by the explicit type
     * @param inferred inferred class
     * @return {@code true} if the assignment is valid, {@code false} otherwise
     */
    public static boolean checkSourceType(Source source, EClass inferred) {
        return getAllFroms(source).anyMatch(f -> checkSourceType(f.getClazz(), inferred));
    }

    /**
     * Get all {@link From froms} contained in {@code source} including the one {@link Source#getFrom() directly contained} as well as those contained in {@link Source#getJoins() joins}.
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
     * Returns whether the given source contains any grouping expressions. Returns {@code false} if no source was provided.
     */
    public static boolean isGrouping(@Nullable Source source) {
        if (source == null) {
            return false;
        }

        return !source.getGroupingExpressions().isEmpty();
    }

    /**
     * Returns the first {@link MultiplicityExpr multiplicity expression} contained in the given features modifiers or {@code null} if none is found.
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
     * Get all queries (main and sub queries) contained in the given view type.
     */
    public static Stream<Query> getAllQueries(ViewTypeDefinition viewType) {
        return viewType.getQueries().stream()
            .flatMap(AstUtils::getAllQueriesImpl);
    }

    private static Stream<Query> getAllQueriesImpl(Query query) {
        if (query.getBody() == null) {
            return Stream.of(query);
        } else {
            var subQueries = query.getBody().getFeatures().stream()
                .map(Feature::getSubQuery)
                .filter(Objects::nonNull)
                .flatMap(AstUtils::getAllQueriesImpl);
            return Stream.concat(Stream.of(query), subQueries);
        }
    }

}
