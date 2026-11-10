package tools.vitruv.neojoin.utils;

import org.eclipse.emf.ecore.EClass;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.aqr.AQRFeatureOptionsBuilder;
import tools.vitruv.neojoin.ast.Body;
import tools.vitruv.neojoin.ast.ConcreteFeature;
import tools.vitruv.neojoin.ast.Feature;
import tools.vitruv.neojoin.ast.FeatureOp;
import tools.vitruv.neojoin.ast.MainQuery;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.model.AQRFeature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Determines the {@link FeatureInfo features} that the target class of a query has from the AST, without creating
 * anything. The rules are the ones that {@link tools.vitruv.neojoin.aqr.AQRBuilder} applies when it
 * creates the
 * features of a target class.
 */
public class FeatureInfoResolver {

    private final ExpressionHelper expressionHelper;
    private final FeatureTypeResolver typeResolver;

    public FeatureInfoResolver(ViewTypeDefinition viewType, ExpressionHelper expressionHelper) {
        this(new FeatureTypeResolver(viewType, expressionHelper), expressionHelper);
    }

    public FeatureInfoResolver(FeatureTypeResolver typeResolver, ExpressionHelper expressionHelper) {
        this.typeResolver = typeResolver;
        this.expressionHelper = expressionHelper;
    }

    /**
     * Determines the features that the target class of the given query will have (without applying inheritance, so
     * an overriding feature has the type and modifiers that are declared for it).
     *
     * @return the features or {@code null} if they cannot be determined
     */
    public @Nullable List<FeatureInfo> getFeatureInfos(Query query) {
        var body = AstUtils.getBody(query);
        if (body != null) {
            return getDeclaredFeatureInfos(query, body);
        }

        // no body: all features are copied from the source class
        var sourceClass = AstUtils.getCopiedSourceClass(query, expressionHelper);
        if (sourceClass == null || sourceClass.eIsProxy()) {
            return null;
        }
        return getCopiedFeatureInfos(query, sourceClass);
    }

    /**
     * Determines the features of the given query like {@link #getFeatureInfos}, but an overriding feature has the
     * type and modifiers of the overridden feature (the first provider), like in
     * {@code AQRBuilder.applyInheritance}.
     *
     * @return the features or {@code null} if they cannot be determined
     */
    public @Nullable List<FeatureInfo> getEffectiveFeatureInfos(Query query) {
        return getEffectiveFeatureInfos(query, new HashSet<>());
    }

    /**
     * @param visiting queries that are currently resolved, to terminate for cyclic inheritance
     */
    private @Nullable List<FeatureInfo> getEffectiveFeatureInfos(Query query, Set<Query> visiting) {
        var declared = getFeatureInfos(query);
        if (declared == null || !(query instanceof MainQuery mainQuery) || !visiting.add(query)) {
            return declared;
        }

        var providers = new LinkedHashMap<String, FeatureInfo>();
        for (var superClass : AstUtils.getResolvedSuperClasses(mainQuery)) {
            var features = getEffectiveFeatureInfos(superClass, visiting);
            if (features != null) {
                features.forEach(feature -> providers.putIfAbsent(feature.name(), feature));
            }
        }
        visiting.remove(query);

        return declared.stream()
            .map(feature -> {
                var provider = providers.get(feature.name());
                return provider == null ? feature : new FeatureInfo(
                    feature.name(), provider.type(), feature.node(), provider.options(), provider.declaringQuery()
                );
            })
            .toList();
    }

    private @Nullable List<FeatureInfo> getDeclaredFeatureInfos(Query query, Body body) {
        var result = new ArrayList<FeatureInfo>();
        for (var feature : AstUtils.getFeatures(body)) {
            var name = AstUtils.getFeatureNameOrNull(feature, expressionHelper);
            if (name == null) {
                return null;
            }
            result.add(new FeatureInfo(name, typeResolver.resolve(feature), feature, getOptionsOrNull(feature), query));
        }
        return result;
    }

    /**
     * Determines the features of the implicit target class of the given source class.
     *
     * @see #getCopiedFeatureInfos(Query, EClass)
     */
    public List<FeatureInfo> getCopiedFeatureInfos(EClass sourceClass) {
        return getCopiedFeatureInfos(null, sourceClass);
    }

    /**
     * Determines the features that are copied by a query without body: all features of the source class, including
     * the inherited ones.
     */
    private List<FeatureInfo> getCopiedFeatureInfos(@Nullable Query query, EClass sourceClass) {
        return sourceClass.getEAllStructuralFeatures().stream()
            .map(feature -> new FeatureInfo(
                feature.getName(),
                typeResolver.resolveCopied(feature),
                null,
                AQRFeatureOptionsBuilder.build(feature),
                query
            ))
            .toList();
    }

    /**
     * Builds the modifiers of a declared feature like {@code AQRBuilder.createFeature}.
     *
     * @return the modifiers or {@code null} if they cannot be determined (e.g. because of other errors)
     */
    private AQRFeature.@Nullable Options getOptionsOrNull(Feature feature) {
        if (!(feature instanceof ConcreteFeature concreteFeature)) {
            return AQRFeatureOptionsBuilder.build(feature, null, AstUtils.isManyMultiplicityDefinition(feature));
        }
        if (concreteFeature.getExpression() == null) {
            return null;
        }

        var inferredType = AstUtils.inferTypeOrNull(concreteFeature.getExpression(), expressionHelper);
        if (inferredType == null) {
            return null;
        }
        var copyFrom = concreteFeature.getOp() == FeatureOp.COPY
            ? AstUtils.getReferencedFeatureOrNull(concreteFeature.getExpression(), expressionHelper)
            : null;

        // AQRFeatureOptionsBuilder.build requires that the cardinality of the modifiers is the one of the expression,
        // which is reported by FeatureModifierValidator otherwise
        var explicitMultiplicity = AstUtils.findMultiplicityExpression(feature);
        var optionsIsMany = explicitMultiplicity != null
            ? AstUtils.isManyMultiplicity(explicitMultiplicity)
            : copyFrom != null ? copyFrom.isMany() : inferredType.isMany();
        if (optionsIsMany != inferredType.isMany()) {
            return null;
        }
        return AQRFeatureOptionsBuilder.build(feature, copyFrom, inferredType.isMany());
    }

}
