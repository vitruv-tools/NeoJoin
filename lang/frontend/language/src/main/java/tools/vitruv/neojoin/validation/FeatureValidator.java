package tools.vitruv.neojoin.validation;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.aqr.AQRFeatureOptionsBuilder;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.ast.Feature;
import tools.vitruv.neojoin.ast.FeatureOp;
import tools.vitruv.neojoin.ast.MainQuery;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.ast.SubQuery;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.jvmmodel.TypeInfo;
import tools.vitruv.neojoin.jvmmodel.TypeResolutionException;
import tools.vitruv.neojoin.utils.AstUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FeatureValidator extends ComposableValidator {

    @Inject
    private ExpressionHelper expressionHelper;

    private @Nullable String getFeatureNameOrNull(Feature feature) {
        if (feature.getName() != null) {
            return feature.getName();
        }

        try {
            var eFeature = expressionHelper.getFeatureOrNull(feature.getExpression());
            if (eFeature != null) {
                return eFeature.getName();
            } // else -> will be handled in checkCopyFeatureExpression
        } catch (TypeResolutionException e) {
            // ignore: will be handled by type checking
        }

        return null;
    }

    @Check
    public void checkUniqueFeatureNames(Query query) {
        if (query.getBody() == null || query.getBody().getFeatures().isEmpty()) {
            return;
        }

        var groupedFeatures = query.getBody().getFeatures().stream()
            .flatMap(feature -> {
                var name = getFeatureNameOrNull(feature);
                if (name != null) {
                    return Stream.of(Map.entry(name, feature));
                } else {
                    return Stream.empty();
                }
            })
            .collect(Collectors.groupingBy(
                Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue, Collectors.toList())
            ));
        groupedFeatures.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .forEach(entry -> {
                for (var feature : entry.getValue()) {
                    error(
                        "Duplicated feature name: " + entry.getKey(),
                        feature,
                        AstPackage.Literals.FEATURE__NAME
                    );
                }
            });
    }

    @Check
    public void checkCopyFeatureExpression(Feature feature) {
        if (feature.getOp() == FeatureOp.COPY) {
            try {
                var eFeature = expressionHelper.getFeatureOrNull(feature.getExpression());
                if (eFeature == null) {
                    error(
                        "Copy feature expression does not reference a feature",
                        feature,
                        AstPackage.Literals.FEATURE__EXPRESSION
                    );
                }
            } catch (TypeResolutionException e) {
                // ignore: will be handled by type checking
            }
        }
    }

    @Inject
    private IBatchTypeResolver typeResolver;

    private LightweightTypeReference getActualType(XExpression expression) {
        return typeResolver.resolveTypes(expression).getActualType(expression);
    }

    @Check
    public void checkFeatureType(Feature feature) {
        TypeInfo inferredType;
        try {
            inferredType = expressionHelper.inferEType(feature.getExpression());
        } catch (TypeResolutionException e) {
            return; // ignore: will be handled by type checking
        }
        if (inferredType == null) {
            error(
                "Unsupported type: " + getActualType(feature.getExpression()),
                feature,
                AstPackage.Literals.FEATURE__EXPRESSION
            );
            return;
        }

        if (inferredType.classifier() == null) { // null expression
            checkNullExpression(feature);
            return;
        }

        if (feature.getSubQuery() != null) {
            checkSubQuery(feature, inferredType.classifier());
        }

        if (feature.getSubQuery() != null && inferredType.classifier() instanceof EDataType dataType) {
            error(
                "Cannot use a subquery with an attribute expression (%s)".formatted(dataType.getName()),
                feature,
                AstPackage.Literals.FEATURE__SUB_QUERY
            );
        }

        if (feature.getType() instanceof Query query) {
            checkQueryType(feature, query, inferredType.classifier());
        } else if (feature.getType() instanceof EEnum eEnum) {
            if (inferredType.classifier() != feature.getType()) {
                error(
                    "Type mismatch: cannot convert from %s to %s".formatted(
                        inferredType.classifier().getName(), eEnum.getName()
                    ),
                    feature,
                    AstPackage.Literals.FEATURE__EXPRESSION
                );
            }
        }
    }

    private void checkNullExpression(Feature feature) {
        if (feature.getType() == null) { // no explicit type
            error("Cannot infer type", feature, AstPackage.Literals.FEATURE__EXPRESSION);
        }
        if (feature.getSubQuery() != null) {
            error("Cannot use null expression for a subquery", feature, AstPackage.Literals.FEATURE__EXPRESSION);
        }
    }

    private void checkSubQuery(Feature feature, @Nullable EClassifier inferredClassifier) {
        if (inferredClassifier instanceof EDataType dataType) {
            error(
                "Cannot use a subquery with an attribute expression (%s)".formatted(dataType.getName()),
                feature,
                AstPackage.Literals.FEATURE__SUB_QUERY
            );
        }
        if (feature.getType() != null && feature.getType() != feature.getSubQuery()) {
            error(
                "Type mismatch: explicit type does not match subquery type",
                feature,
                AstPackage.Literals.FEATURE__TYPE
            );
        }
    }

    private void checkQueryType(Feature feature, Query explicitType, EClassifier inferredClassifier) {
        if (explicitType instanceof MainQuery mainQuery) {
            if (mainQuery.getSource() != null && inferredClassifier instanceof EClass inferredClass) {
                if (AstUtils.checkSourceType(mainQuery.getSource(), inferredClass)) {
                    return;
                }
            }
        } else {
            var subQuery = (SubQuery) explicitType;

            if (inferredClassifier instanceof EClass inferredClass) {
                var subQuerySourceType = AstUtils.inferSubQuerySourceType(subQuery, expressionHelper);
                if (subQuerySourceType == null) {
                    return; // will be handled elsewhere
                }
                if (AstUtils.checkSourceType(subQuerySourceType, inferredClass)) {
                    return;
                }
            }
        }

        error(
            "Type mismatch: cannot convert from %s to %s".formatted(
                inferredClassifier.getName(), AstUtils.getTargetName(explicitType, expressionHelper)
            ),
            feature,
            AstPackage.Literals.FEATURE__EXPRESSION
        );
    }

    @Check
    public void checkFeatureNullability(Feature feature) {
        if (feature.getExpression() instanceof XMemberFeatureCall featureCall) {
            if (featureCall.isNullSafe() && isRequired(feature)) {
                warning(
                    "Nullable expression used to initialize non-nullable feature",
                    feature,
                    AstPackage.Literals.FEATURE__EXPRESSION
                );
            }
        }
    }

    private boolean isRequired(Feature feature) {
        EStructuralFeature copyFrom = null;
        if (feature.getOp() == FeatureOp.COPY) {
            try {
                copyFrom = expressionHelper.getFeatureOrNull(feature.getExpression());
            } catch (TypeResolutionException e) {
                return false; // ignore: will be handled by type checking
            }
        }
        // we only care about the lower bound, so it does not matter what we pass for `inferredIsMany`
        var options = AQRFeatureOptionsBuilder.build(feature, copyFrom, false);
        return options.isRequired();
    }

    @Check
    public void checkImplicitFeatureTypes(ViewTypeDefinition viewType) {
        var sourceMap = new HashMap<EClass, Set<Query>>();
        BiConsumer<EClass, Query> register = (sourceType, query) -> {
            sourceMap.computeIfAbsent(sourceType, k -> new HashSet<>()).add(query);
        };

        AstUtils.getAllQueries(viewType).forEach(query -> {
            if (query instanceof MainQuery mainQuery) {
                if (mainQuery.getSource() != null) {
                    AstUtils.getAllFroms(mainQuery.getSource()).forEach(f -> {
                        register.accept(f.getClazz(), mainQuery);
                    });
                }
            } else if (query instanceof SubQuery subQuery) {
                var sourceType = AstUtils.inferSubQuerySourceType(subQuery, expressionHelper);
                if (sourceType != null) {
                    register.accept(sourceType, subQuery);
                }
            }
        });

        AstUtils.getAllQueries(viewType)
            .filter(q -> q.getBody() != null)
            .flatMap(q -> q.getBody().getFeatures().stream())
            .filter(f -> f.getType() == null) // explicit type -> not ambiguous
            .filter(f -> f.getSubQuery() == null) // subquery -> not ambiguous
            .forEach(f -> checkImplicitFeatureType(f, sourceMap));
    }

    private void checkImplicitFeatureType(Feature feature, Map<EClass, Set<Query>> sourceMap) {
        try {
            var inferredType = expressionHelper.inferEType(feature.getExpression());
            if (inferredType == null || !(inferredType.classifier() instanceof EClass inferredClass)) {
                return;
            }

            var candidates = sourceMap.get(inferredClass);
            if (candidates == null) {
                return; // -> implicit copy of target class
            }

            if (candidates.size() > 1) {
                error(
                    "Ambiguous target class for source class '%s'. Possible candidates: %s".formatted(
                        inferredClass.getName(),
                        candidates.stream().map(q -> AstUtils.getTargetName(q, expressionHelper)).sorted()
                            .collect(Collectors.joining(", "))
                    ),
                    feature,
                    AstPackage.Literals.FEATURE__EXPRESSION
                );
            } else if (candidates.size() == 1) {
                if (candidates.iterator().next() instanceof MainQuery mainQuery && mainQuery.getSource() != null) {
                    if (!mainQuery.getSource().getJoins().isEmpty()) {
                        warning(
                            "Inferred type '%s' is a query with join which might be unintended and can lead to errors during transformation. Use explicit type to clarify the intended type.".formatted(
                                AstUtils.getTargetName(mainQuery)),
                            feature,
                            null
                        );
                    } else if (!mainQuery.getSource().getGroupingExpressions().isEmpty()) {
                        warning(
                            "Inferred type '%s' is a query with group by which might be unintended and can lead to errors during transformation. Use explicit type to clarify the intended type.".formatted(
                                AstUtils.getTargetName(mainQuery)),
                            feature,
                            null
                        );
                    }
                }
            }
        } catch (TypeResolutionException e) {
            // ignore: will be handled by type checking
        }
    }

    @Check
    public void checkRootFeatureCollision(MainQuery mainQuery) {
        if (!mainQuery.isRoot() || mainQuery.getBody() == null) {
            return;
        }

        var allNames = AstUtils.getAllQueries(AstUtils.getViewType(mainQuery))
            .map(q -> AstUtils.getTargetName(q, expressionHelper))
            .map(Constants.RootReferenceNameFormat::formatted)
            .collect(Collectors.toSet());

        for (var feature : mainQuery.getBody().getFeatures()) {
            if (allNames.contains(feature.getName())) {
                error(
                    "Feature name collides with implicit root containment reference.",
                    feature,
                    AstPackage.Literals.FEATURE__NAME
                );
            }
        }
    }

}
