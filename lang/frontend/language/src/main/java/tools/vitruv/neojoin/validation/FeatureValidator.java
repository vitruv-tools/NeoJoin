package tools.vitruv.neojoin.validation;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
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
import tools.vitruv.neojoin.ast.ConcreteBody;
import tools.vitruv.neojoin.ast.ConcreteFeature;
import tools.vitruv.neojoin.ast.ConcreteMainQuery;
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
import tools.vitruv.neojoin.utils.EMFUtils;
import tools.vitruv.neojoin.utils.FeatureInfo;
import tools.vitruv.neojoin.utils.FeatureInfoResolver;
import tools.vitruv.neojoin.utils.FeatureType.AmbiguousReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.ImplicitReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.QueryReferenceType;
import tools.vitruv.neojoin.utils.FeatureTypeResolver;
import tools.vitruv.neojoin.utils.ImplicitClassCollector;
import tools.vitruv.neojoin.utils.Pair;
import tools.vitruv.neojoin.utils.TypeCasts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FeatureValidator extends ComposableValidator {

    @Inject
    private ExpressionHelper expressionHelper;

    @Check
    public void checkUniqueFeatureNames(Query query) {
        var body = AstUtils.getBody(query);
        if (body == null) {
            return;
        }

        var features = AstUtils.getFeatures(body);
        if (features.isEmpty()) {
            return;
        }

        var groupedFeatures = features.stream()
            .flatMap(feature -> {
                var name = AstUtils.getFeatureNameOrNull(feature, expressionHelper);
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
    public void checkCopyFeatureExpression(ConcreteFeature feature) {
        if (feature.getOp() == FeatureOp.COPY) {
            try {
                var eFeature = expressionHelper.getFeatureOrNull(feature.getExpression());
                if (eFeature == null) {
                    error(
                        "Copy feature expression does not reference a feature",
                        feature,
                        AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
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
    public void checkFeatureType(ConcreteFeature feature) {
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
                AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
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
                AstPackage.Literals.CONCRETE_FEATURE__SUB_QUERY
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
                    AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
                );
            }
        } else if (feature.getType() instanceof EDataType eDataType) {
            if (inferredType.classifier() instanceof EDataType && !(inferredType.classifier() instanceof EEnum)) {
                if (!TypeCasts.canCast(inferredType.classifier().getInstanceClass(), eDataType.getInstanceClass())) {
                    error(
                        "Type mismatch: cannot convert from %s (%s) to %s (%s)".formatted(
                            inferredType.classifier().getName(), inferredType.classifier().getInstanceClass().getSimpleName(),
                            eDataType.getName(), eDataType.getInstanceClass().getSimpleName()
                        ),
                        feature,
                        AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
                    );
                }
            } else {
                error(
                    "Type mismatch: cannot convert from %s to %s".formatted(
                        inferredType.classifier().getName(), eDataType.getName()
                    ),
                    feature,
                    AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
                );
            }
        }
    }

    private void checkNullExpression(ConcreteFeature feature) {
        if (feature.getType() == null) { // no explicit type
            error("Cannot infer type", feature, AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION);
        }
        if (feature.getSubQuery() != null) {
            error("Cannot use null expression for a subquery", feature, AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION);
        }
    }

    private void checkSubQuery(ConcreteFeature feature, @Nullable EClassifier inferredClassifier) {
        if (inferredClassifier instanceof EDataType dataType) {
            error(
                "Cannot use a subquery with an attribute expression (%s)".formatted(dataType.getName()),
                feature,
                AstPackage.Literals.CONCRETE_FEATURE__SUB_QUERY
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

    private void checkQueryType(ConcreteFeature feature, Query explicitType, EClassifier inferredClassifier) {
        if (inferredClassifier instanceof EClass inferredClass) {
            var sourceMap = AstUtils.mapSourceClassesToQueries(AstUtils.getViewType(feature), expressionHelper);
            // the target class of the inferred type must be unambiguously assignable to the explicit type, which
            // mirrors the invariant in AQRBuilder.determineReferenceType (otherwise, the transformation could not
            // decide which target instance to reference)
            var assignable = sourceMap.getOrDefault(inferredClass, Set.of()).stream()
                .filter(type -> AstUtils.isSameOrSuperClassOf(explicitType, type))
                .toList();
            if (assignable.size() > 1) {
                error(
                    "Ambiguous target class for source class '%s'. Possible candidates: %s".formatted(
                        inferredClass.getName(),
                        assignable.stream().map(q -> AstUtils.getTargetName(q, expressionHelper)).sorted()
                            .collect(Collectors.joining(", "))
                    ),
                    feature,
                    AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
                );
                return;
            } else if (assignable.size() == 1) {
                return;
            }

            // no query selects exactly the inferred class with a matching target class: sub class instances (or
            // instances selected via 'from extends') of the inferred class can still be selected by a query whose
            // target class is the explicit type or a sub class of it
            var viewType = AstUtils.getViewType(feature);
            if (AstUtils.getAllQueries(viewType).anyMatch(query ->
                AstUtils.canSelectInstancesOf(query, inferredClass, expressionHelper)
                    && AstUtils.isSameOrSuperClassOf(explicitType, query)
            )) {
                return;
            }
        }

        if (explicitType instanceof SubQuery subQuery) {
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
            AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
        );
    }

    @Check
    public void checkFeatureNullability(ConcreteFeature feature) {
        if (feature.eContainer().eContainer() instanceof MainQuery mainQuery && !mainQuery.getSuperClasses().isEmpty()) {
            return; // checked by InheritanceValidator, because overriding features have the modifiers of the overridden feature
        }

        if (feature.getExpression() instanceof XMemberFeatureCall featureCall) {
            if (featureCall.isNullSafe() && isRequired(feature)) {
                warning(
                    "Nullable expression used to initialize non-nullable feature",
                    feature,
                    AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
                );
            }
        }
    }

    private boolean isRequired(Feature feature) {
        EStructuralFeature copyFrom = null;
        if (feature instanceof ConcreteFeature concreteFeature && concreteFeature.getOp() == FeatureOp.COPY) {
            try {
                copyFrom = expressionHelper.getFeatureOrNull(concreteFeature.getExpression());
            } catch (TypeResolutionException e) {
                return false; // ignore: will be handled by type checking
            }
        }
        // we only care about the lower bound, so it does not matter what we pass for `inferredIsMany`
        var options = AQRFeatureOptionsBuilder.build(feature, copyFrom, false);
        return options.isRequired();
    }

    @Check
    public void checkAmbiguousImplicitFeatureTypes(ViewTypeDefinition viewType) {
        var typeResolver = new FeatureTypeResolver(viewType, expressionHelper);

        var alreadyChecked = new HashSet<EClass>(); // to prevent infinite recursion because of cyclic references
        AstUtils.getAllQueries(viewType)
            .forEach(q -> checkQuery(q, typeResolver, alreadyChecked));
    }

    private void checkQuery(Query query, FeatureTypeResolver typeResolver, HashSet<EClass> alreadyChecked) {
        var body = AstUtils.getBody(query);
        if (body == null) {
            // a query without body and either no source or a source with joins is not allowed,
            // but skip here because this is handled elsewhere
            var sourceType = AstUtils.getCopiedSourceClass(query, expressionHelper);
            if (sourceType != null) {
                checkCopiedClass(sourceType, typeResolver, query, alreadyChecked);
            }
        } else if (body instanceof ConcreteBody concreteBody) {
            concreteBody.getFeatures().stream()
                .filter(f -> f.getType() == null) // explicit type -> not ambiguous
                .filter(f -> f.getSubQuery() == null) // subquery -> not ambiguous
                .forEach(f -> checkImplicitFeatureType(f, typeResolver, alreadyChecked));
        }
    }

    private void checkImplicitFeatureType(ConcreteFeature feature, FeatureTypeResolver typeResolver, HashSet<EClass> alreadyChecked) {
        var type = typeResolver.resolve(feature);
        if (type instanceof ImplicitReferenceType implicitType) { // implicit copy of target class
            checkCopiedClass(implicitType.sourceClass(), typeResolver, (Query) feature.eContainer().eContainer(), alreadyChecked);
        } else if (type instanceof AmbiguousReferenceType ambiguousType) {
            error(
                "Ambiguous target class for source class '%s'. Possible candidates: %s".formatted(
                    ambiguousType.sourceClass().getName(),
                    ambiguousType.candidates().stream().map(q -> AstUtils.getTargetName(q, expressionHelper)).sorted()
                        .collect(Collectors.joining(", "))
                ),
                feature,
                AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
            );
        } else if (type instanceof QueryReferenceType queryType) {
            if (queryType.query() instanceof ConcreteMainQuery mainQuery && mainQuery.getSource() != null) {
                if (!mainQuery.getSource().getJoins().isEmpty()) {
                    warning(
                        "Inferred type '%s' is a query with join which might be unintended and can lead to errors during transformation. Use explicit type to clarify the intended type.".formatted(
                            AstUtils.getTargetName(mainQuery)),
                        feature,
                        AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
                    );
                } else if (!mainQuery.getSource().getGroupingExpressions().isEmpty()) {
                    warning(
                        "Inferred type '%s' is a query with group by which might be unintended and can lead to errors during transformation. Use explicit type to clarify the intended type.".formatted(
                            AstUtils.getTargetName(mainQuery)),
                        feature,
                        AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
                    );
                }
            }
        }
    }

    private void checkCopiedClass(EClass clazz, FeatureTypeResolver typeResolver, Query cause, HashSet<EClass> alreadyChecked) {
        var wasAlreadyChecked = !alreadyChecked.add(clazz);
        if (wasAlreadyChecked) {
            return;
        }

        for (var reference : clazz.getEAllReferences()) {
            var type = typeResolver.resolveSourceClass(reference.getEReferenceType());
            if (type instanceof ImplicitReferenceType) { // -> implicit copy of target class
                checkCopiedClass(reference.getEReferenceType(), typeResolver, cause, alreadyChecked);
            } else if (type instanceof AmbiguousReferenceType ambiguousType) {
                error(
                    "Ambiguous target class for source class '%s' while copying reference '%s::%s'. Possible candidates: %s".formatted(
                        reference.getEReferenceType().getName(),
                        clazz.getName(), reference.getName(),
                        ambiguousType.candidates().stream().map(q -> AstUtils.getTargetName(q, expressionHelper)).sorted()
                            .collect(Collectors.joining(", "))
                    ),
                    cause,
                    null
                );
            } else if (type instanceof QueryReferenceType queryType) {
                if (queryType.query() instanceof ConcreteMainQuery mainQuery && mainQuery.getSource() != null) {
                    if (!mainQuery.getSource().getJoins().isEmpty()) {
                        warning(
                            ("Inferred target class '%s' for source class '%s' while copying reference '%s::%s' is " +
                                "a query with join which might be unintended and can lead to errors during transformation. " +
                                "Use explicit type to clarify the intended type.").formatted(
                                AstUtils.getTargetName(mainQuery),
                                mainQuery.getSource().getFrom().getClazz().getName(),
                                clazz.getName(), reference.getName()
                            ),
                            cause,
                            null
                        );
                    } else if (!mainQuery.getSource().getGroupingExpressions().isEmpty()) {
                        warning(
                            ("Inferred target class '%s' for source class '%s' while copying reference '%s::%s' is " +
                                "a query with group by which might be unintended and can lead to errors during transformation. " +
                                "Use explicit type to clarify the intended type.").formatted(
                                AstUtils.getTargetName(mainQuery),
                                mainQuery.getSource().getFrom().getClazz().getName(),
                                clazz.getName(), reference.getName()
                            ),
                            cause,
                            null
                        );
                    }
                }
            }
        }
    }

    /**
     * Checks the implicit super class of implicit target classes: it must be unambiguous, and the implicit class
     * should have all features of it.
     * <p>
     * {@code AQRBuilder} links these classes (they cannot declare {@code extends}) to the target class of their
     * nearest source super class that has one: a query that selects exactly this source class (single {@code from},
     * no joins, no grouping, including subqueries), or an implicit target class. It fails if several target classes
     * exist on the nearest level of the source class hierarchy, so this is reported here instead. The check in
     * {@code AQRBuilder.findImplicitSuperClass} stays as the safeguard for queries that were not validated (it
     * decides which class is linked, while this method only reports the ambiguity to the user).
     * <p>
     * An implicit class has the features of its source class, so it cannot override features of the candidate that
     * are not features of the source class. {@code AQRBuilder.linkImplicitSuperClasses} does not link the candidate
     * then (the class would violate the rule that sub classes override all inherited features), which is reported as
     * a warning here.
     * <p>
     * The implicit classes are determined from the AST like in {@link #checkAmbiguousImplicitFeatureTypes}: classes of
     * references (copied or inferred) for which no query selects the referenced source class. The issues are reported
     * at the feature that references the class, or at the query if the references are copied.
     */
    @Check
    public void checkAmbiguousImplicitSuperClasses(ViewTypeDefinition viewType) {
        var queriesBySourceClass = AstUtils.mapSourceClassesToQueries(viewType, expressionHelper);
        var typeResolver = new FeatureTypeResolver(queriesBySourceClass, expressionHelper);
        var featureInfoResolver = new FeatureInfoResolver(typeResolver, expressionHelper);

        var implicitClasses = new ImplicitClassCollector(typeResolver, expressionHelper).collectImplicitClasses(viewType);

        implicitClasses.forEach((sourceClass, cause) -> {
            var candidates = findNearestSuperClassCandidates(sourceClass, queriesBySourceClass, implicitClasses.keySet());
            if (candidates.size() > 1) {
                error(
                    "Ambiguous super class for implicit target class of source class '%s'. Possible candidates: %s".formatted(
                        sourceClass.getName(), candidates.stream().map(Pair::left).collect(Collectors.joining(", "))
                    ),
                    cause,
                    null
                );
            } else if (candidates.size() == 1 && candidates.get(0).right() != null) {
                checkImplicitClassOverridesAllFeatures(
                    sourceClass, candidates.get(0).left(), candidates.get(0).right(), cause, featureInfoResolver
                );
            }
        });
    }

    private void checkImplicitClassOverridesAllFeatures(
        EClass sourceClass,
        String superClassName,
        Query superClass,
        EObject cause,
        FeatureInfoResolver featureInfoResolver
    ) {
        var superClassFeatures = featureInfoResolver.getEffectiveFeatureInfos(superClass);
        if (superClassFeatures == null) {
            return; // features cannot be determined: other errors will be reported
        }

        var ownNames = sourceClass.getEAllStructuralFeatures().stream()
            .map(EStructuralFeature::getName)
            .collect(Collectors.toSet());
        var missingNames = superClassFeatures.stream()
            .map(FeatureInfo::name)
            .distinct()
            .filter(name -> !ownNames.contains(name))
            .toList();
        if (!missingNames.isEmpty()) {
            warning(
                "Implicit target class of source class '%s' is not linked to super class '%s', because it does not override: %s".formatted(
                    sourceClass.getName(), superClassName, String.join(", ", missingNames)
                ),
                cause,
                null
            );
        }
    }

    /**
     * Returns the target classes (name and query, the query is {@code null} for an implicit class) of the nearest
     * level of super classes of the given source class (breadth first) that has any, sorted by name.
     */
    private List<Pair<String, @Nullable Query>> findNearestSuperClassCandidates(
        EClass sourceClass,
        Map<EClass, Set<Query>> queriesBySourceClass,
        Set<EClass> implicitClasses
    ) {
        return EMFUtils.findInNearestSuperClasses(
            sourceClass, superClass -> {
                var candidates = queriesBySourceClass.getOrDefault(superClass, Set.of()).stream()
                    .filter(q -> superClass.equals(AstUtils.getSingleSourceClass(q, expressionHelper)))
                    .map(q -> new Pair<String, @Nullable Query>(AstUtils.getTargetName(q, expressionHelper), q))
                    .collect(Collectors.toCollection(ArrayList::new));
                if (implicitClasses.contains(superClass)) {
                    candidates.add(new Pair<>(superClass.getName(), null));
                }
                return candidates;
            }
        ).stream().sorted(Comparator.comparing(Pair::left)).toList();
    }

    @Check
    public void checkRootFeatureCollision(ConcreteMainQuery mainQuery) {
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
