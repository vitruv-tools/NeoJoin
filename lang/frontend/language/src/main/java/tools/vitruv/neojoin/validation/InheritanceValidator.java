package tools.vitruv.neojoin.validation;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.validation.Check;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.ast.Body;
import tools.vitruv.neojoin.ast.ConcreteFeature;
import tools.vitruv.neojoin.ast.ConcreteMainQuery;
import tools.vitruv.neojoin.ast.Feature;
import tools.vitruv.neojoin.ast.FeatureOp;
import tools.vitruv.neojoin.ast.MainQuery;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.jvmmodel.TypeResolutionException;
import tools.vitruv.neojoin.utils.AstUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates the inheritance relations between target classes (see {@code extends} in queries).
 * <p>
 * The checks mirror the invariants of {@link tools.vitruv.neojoin.aqr.AQRBuilder} (mainly
 * {@code addSuperClassesToTargetClass}, {@code applyInheritance} and {@code determineReferenceType}) so that invalid
 * queries are reported as issues instead of failing with an {@code AQRInvariantViolatedException} during AQR
 * construction. They also protect the later stages, which rely on a well-formed class hierarchy:
 * {@link tools.vitruv.neojoin.generation.MetaModelGenerator} maps the super classes to {@code eSuperTypes} (which
 * must be acyclic and duplicate free), and the transformation resolves references to instances of sub classes via
 * {@code AQRTargetClass.allSuperClasses()} (see {@code TargetMap.get(..., allowSubclasses)} in the EMF backend) and
 * evaluates overriding features via {@code AQRFeature.Kind.Override} ({@code Transformator.evaluateFeature}).
 * <p>
 * Note that the rules only handle inheritance. The validation of features and types in general is done by
 * {@link FeatureValidator} and {@link FeatureModifierValidator}.
 */
public class InheritanceValidator extends ComposableValidator {

    @Inject
    private ExpressionHelper expressionHelper;

    private sealed interface FeatureType {}

    /**
     * @param type the data type of the attribute
     */
    private record AttributeType(EDataType type) implements FeatureType {}

    private sealed interface ReferenceType extends FeatureType {}

    /**
     * @param query the {@link Query} that creates the target class which is the type of the reference
     */
    private record QueryReferenceType(Query query) implements ReferenceType {}

    /**
     * @param sourceClass the source class that is copied to an implicitly created target class which is the type of
     *                    the reference
     */
    private record ImplicitReferenceType(EClass sourceClass) implements ReferenceType {}

    /**
     * @param name name of the feature
     * @param type type of the feature or {@code null} if it could not be determined
     * @param node AST node of the feature or {@code null} for implicitly copied features
     */
    private record FeatureInfo(String name, @Nullable FeatureType type, @Nullable Feature node) {}

    // ---- super class list

    /**
     * Checks that a class does not (transitively) extend itself.
     * <p>
     * A class that extends itself has no finite set of super classes: the generated {@code eSuperTypes} would be
     * cyclic (invalid Ecore), and walking the hierarchy ({@code AQRTargetClass.allSuperClasses()},
     * {@code AstUtils.isSameOrSuperTypeOf}) would not terminate or would produce meaningless results.
     */
    @Check
    public void checkAcyclicInheritance(MainQuery query) {
        var superClasses = query.getSuperClasses();
        for (var i = 0; i < superClasses.size(); i++) {
            if (!(superClasses.get(i) instanceof Query superQuery) || superQuery.eIsProxy()) {
                continue; // unresolved: will be handled by linking
            }

            if (superQuery == query) {
                error("A class cannot extend itself", query, AstPackage.Literals.MAIN_QUERY__SUPER_CLASSES, i);
            } else if (AstUtils.isSameOrSuperTypeOf(query, superQuery)) {
                error("Cyclic inheritance", query, AstPackage.Literals.MAIN_QUERY__SUPER_CLASSES, i);
            }
        }
    }

    /**
     * Checks that a class lists each super class only once.
     * <p>
     * A duplicated super class would be added twice to {@code AQRTargetClass.superClasses()}
     * ({@code AQRBuilder.addSuperClassesToTargetClass}) and to the {@code eSuperTypes} of the generated class.
     */
    @Check
    public void checkDuplicatedSuperClasses(MainQuery query) {
        var superClasses = query.getSuperClasses();
        for (var i = 0; i < superClasses.size(); i++) {
            var superClass = superClasses.get(i);
            if (!(superClass instanceof Query superQuery) || superQuery.eIsProxy()) {
                continue; // unresolved: will be handled by linking
            }

            if (superClasses.stream().filter(other -> other == superClass).count() > 1) {
                error(
                    "Duplicated super class: " + AstUtils.getTargetName(superQuery, expressionHelper),
                    query,
                    AstPackage.Literals.MAIN_QUERY__SUPER_CLASSES,
                    i
                );
            }
        }
    }

    /**
     * Warns if a class extends the root class.
     * <p>
     * The containment references of the root class are generated last ({@code AQRBuilder.populateRoot}, after
     * {@code applyInheritance}), so they are not part of the features that the sub class has to override. The sub
     * class would nevertheless inherit them in the generated view type, which is rarely intended.
     */
    @Check
    public void checkExtendingRoot(MainQuery query) {
        var superClasses = query.getSuperClasses();
        for (var i = 0; i < superClasses.size(); i++) {
            if (superClasses.get(i) instanceof ConcreteMainQuery superQuery && superQuery.isRoot()) {
                warning(
                    "Class extends the root class and inherits all root containment references",
                    query,
                    AstPackage.Literals.MAIN_QUERY__SUPER_CLASSES,
                    i
                );
            }
        }
    }

    // ---- inherited features

    /**
     * Checks that a feature is not inherited from multiple super classes.
     * <p>
     * {@code AQRBuilder.applyInheritance} requires exactly one overridden feature per feature name
     * ({@code invariant(overriddenFeatures.size() == 1)}), so diamonds or name conflicts cannot be resolved (this is
     * a current limitation).
     */
    @Check
    public void checkFeatureInheritedFromSingleSuperClass(MainQuery query) {
        var inherited = getInheritedFeatures(query);
        for (var entry : getSuperClassesByFeatureName(inherited).entrySet()) {
            var superClasses = entry.getValue();
            if (superClasses.size() > 1) {
                var classNames = superClasses.stream()
                    .map(c -> AstUtils.getTargetName(c, expressionHelper))
                    .collect(Collectors.joining(", "));
                for (var superClass : superClasses) {
                    errorAtSuperClass(
                        "Feature '%s' is inherited from multiple super classes: %s".formatted(entry.getKey(), classNames),
                        query,
                        superClass
                    );
                }
            }
        }
    }

    /**
     * Checks that a class declares every inherited feature again.
     * <p>
     * Without a declaration, the transformation would have no way to compute the value
     * ({@code Transformator.evaluateFeature} only evaluates the overriding kind of an
     * {@code AQRFeature.Kind.Override}). {@code AQRBuilder.applyInheritance} requires this ("Sub classes must
     * override all inherited features").
     */
    @Check
    public void checkInheritedFeaturesAreOverridden(MainQuery query) {
        var inherited = getInheritedFeatures(query);
        var own = getOwnFeatures(query);
        if (own == null) {
            return; // features cannot be determined: other errors will be reported
        }

        var ownNames = own.stream().map(FeatureInfo::name).collect(Collectors.toSet());
        inherited.forEach((superClass, features) -> {
            features.stream()
                .map(FeatureInfo::name)
                .distinct()
                .filter(name -> !ownNames.contains(name))
                .forEach(name -> errorAtSuperClass(
                    "Missing override of inherited feature '%s' from '%s'".formatted(
                        name, AstUtils.getTargetName(superClass, expressionHelper)
                    ),
                    query,
                    superClass
                ));
        });
    }

    /**
     * Warns about modifiers on overriding features.
     * <p>
     * {@code AQRBuilder.applyInheritance} replaces the modifiers of an overriding feature by the modifiers of the
     * overridden feature ({@code withOptions}), so modifiers written on the overriding feature have no effect.
     */
    @Check
    public void checkOverridingFeatureModifiers(MainQuery query) {
        var overriddenNames = getSuperClassesByFeatureName(getInheritedFeatures(query)).keySet();
        var own = getOwnFeatures(query);
        if (own == null) {
            return; // features cannot be determined: other errors will be reported
        }

        for (var feature : own) {
            if (overriddenNames.contains(feature.name()) && feature.node() != null && !feature.node().getModifiers().isEmpty()) {
                warning(
                    "Modifiers of overriding feature '%s' are ignored".formatted(feature.name()),
                    feature.node(),
                    AstPackage.Literals.FEATURE__MODIFIERS
                );
            }
        }
    }

    /**
     * Checks that overriding features are compatible with the overridden features.
     * <p>
     * Attributes must override attributes of the same data type, and references must override references. An
     * overriding feature has the type of the overridden feature (a stricter type would break setting the reference
     * through the super class): an explicit type must be equal to it, and the type of the value must be equal to it or
     * a sub class (invariants in {@code AQRBuilder.applyInheritance}). Otherwise, the generated view type would be
     * invalid, and values could not be assigned to the inherited feature during the transformation.
     * Features that are inherited from multiple super classes are skipped (see
     * {@link #checkFeatureInheritedFromSingleSuperClass}).
     */
    @Check
    public void checkOverridingFeatureTypes(MainQuery query) {
        var inherited = getInheritedFeatures(query);
        var superClassesByName = getSuperClassesByFeatureName(inherited);
        var own = getOwnFeatures(query);
        if (own == null) {
            return; // features cannot be determined: other errors will be reported
        }

        for (var feature : own) {
            var superClasses = superClassesByName.get(feature.name());
            if (superClasses == null || superClasses.size() != 1) {
                continue; // does not override or reported by checkFeatureInheritedFromSingleSuperClass
            }

            var superClass = superClasses.get(0);
            var overridden = findFeature(inherited.get(superClass), feature.name());
            var hasExplicitType = feature.node() != null && feature.node().getType() != null;
            var message = getOverrideViolation(feature.name(), feature.type(), hasExplicitType, overridden.type());
            if (message != null) {
                if (feature.node() != null) {
                    error(
                        message,
                        feature.node(),
                        feature.node().getName() != null ? AstPackage.Literals.FEATURE__NAME : null
                    );
                } else {
                    errorAtSuperClass(message, query, superClass);
                }
            }
        }
    }

    // ---- helpers for the checks

    /**
     * Reports an error at the given entry of the {@code extends} list.
     */
    private void errorAtSuperClass(String message, MainQuery query, Query superClass) {
        error(
            message,
            query,
            AstPackage.Literals.MAIN_QUERY__SUPER_CLASSES,
            query.getSuperClasses().indexOf(superClass)
        );
    }

    /**
     * Returns the distinct, resolved super classes of the given query.
     */
    private static List<Query> getResolvedSuperClasses(MainQuery query) {
        var result = new LinkedHashSet<Query>();
        for (var superClass : query.getSuperClasses()) {
            if (superClass instanceof Query superQuery && !superQuery.eIsProxy() && superQuery != query) {
                result.add(superQuery);
            }
        }
        return List.copyOf(result);
    }

    private static boolean hasCyclicSuperClasses(MainQuery query) {
        return query.getSuperClasses().stream().anyMatch(superClass ->
            superClass instanceof Query superQuery
                && !superQuery.eIsProxy()
                && (superQuery == query || AstUtils.isSameOrSuperTypeOf(query, superQuery))
        );
    }

    private static FeatureInfo findFeature(List<FeatureInfo> features, String name) {
        return features.stream().filter(feature -> feature.name().equals(name)).findFirst().orElseThrow();
    }

    /**
     * Returns the features of the super classes by super class. Super classes whose features cannot be determined are
     * left out. The result is empty if the class has no super classes or if the inheritance is cyclic (reported by
     * {@link #checkAcyclicInheritance}).
     */
    private Map<Query, List<FeatureInfo>> getInheritedFeatures(MainQuery query) {
        var superClasses = getResolvedSuperClasses(query);
        if (superClasses.isEmpty() || hasCyclicSuperClasses(query)) {
            return Map.of();
        }

        var sourceMap = AstUtils.mapSourceClassesToQueries(AstUtils.getViewType(query), expressionHelper);
        var inherited = new LinkedHashMap<Query, List<FeatureInfo>>();
        for (var superClass : superClasses) {
            var features = getFeatureInfos(superClass, sourceMap);
            if (features != null) {
                inherited.put(superClass, features);
            }
        }
        return inherited;
    }

    /**
     * Returns the features that the target class of the given query has, including the features that override
     * inherited features.
     *
     * @return the features or {@code null} if they cannot be determined
     */
    private @Nullable List<FeatureInfo> getOwnFeatures(MainQuery query) {
        var sourceMap = AstUtils.mapSourceClassesToQueries(AstUtils.getViewType(query), expressionHelper);
        return getFeatureInfos(query, sourceMap);
    }

    /**
     * Returns the super classes that provide a feature, by name of the feature.
     */
    private static Map<String, List<Query>> getSuperClassesByFeatureName(Map<Query, List<FeatureInfo>> inherited) {
        var superClassesByName = new LinkedHashMap<String, List<Query>>();
        inherited.forEach((superClass, features) -> {
            features.stream()
                .map(FeatureInfo::name)
                .distinct()
                .forEach(name -> superClassesByName.computeIfAbsent(name, k -> new ArrayList<>()).add(superClass));
        });
        return superClassesByName;
    }

    /**
     * Mirrors the checks of {@link tools.vitruv.neojoin.aqr.AQRBuilder} for overriding features.
     *
     * @return error message or {@code null} if valid or unknown
     */
    private static @Nullable String getOverrideViolation(
        String name,
        @Nullable FeatureType overriding,
        boolean hasExplicitType,
        @Nullable FeatureType overridden
    ) {
        if (overriding == null || overridden == null) {
            return null;
        }

        if (overriding instanceof AttributeType attribute) {
            if (!(overridden instanceof AttributeType overriddenAttribute)) {
                return "Attribute '%s' cannot override a reference".formatted(name);
            }
            if (!attribute.type().equals(overriddenAttribute.type())) {
                return "Type of overriding feature '%s' must be equal to the type of the overridden feature"
                    .formatted(name);
            }
        } else if (overriding instanceof ReferenceType reference) {
            if (!(overridden instanceof ReferenceType overriddenReference)) {
                return "Reference '%s' cannot override an attribute".formatted(name);
            }
            if (hasExplicitType && !reference.equals(overriddenReference)) {
                return "Type of overriding feature '%s' must be equal to the type of the overridden feature"
                    .formatted(name);
            }
            if (!isSameOrSubType(reference, overriddenReference)) {
                return "Value of overriding feature '%s' cannot be assigned to the type of the overridden feature"
                    .formatted(name);
            }
        }
        return null;
    }

    private static boolean isSameOrSubType(ReferenceType subType, ReferenceType superType) {
        return subType.equals(superType) || (
            subType instanceof QueryReferenceType sub
                && superType instanceof QueryReferenceType sup
                && AstUtils.isSameOrSuperTypeOf(sup.query(), sub.query())
        );
    }

    // ---- features and types of queries

    /**
     * Determines the features that the target class of the given query will have.
     *
     * @return the features or {@code null} if they cannot be determined
     */
    private @Nullable List<FeatureInfo> getFeatureInfos(Query query, Map<EClass, Set<Query>> sourceMap) {
        var body = AstUtils.getBody(query);
        if (body != null) {
            return getDeclaredFeatureInfos(body, sourceMap);
        }

        // no body: all features are copied from the source class
        var sourceClass = AstUtils.getCopiedSourceClass(query, expressionHelper);
        if (sourceClass == null || sourceClass.eIsProxy()) {
            return null;
        }
        return getCopiedFeatureInfos(sourceClass, sourceMap);
    }

    private @Nullable List<FeatureInfo> getDeclaredFeatureInfos(
        Body body,
        Map<EClass, Set<Query>> sourceMap
    ) {
        var result = new ArrayList<FeatureInfo>();
        for (var feature : AstUtils.getFeatures(body)) {
            var name = AstUtils.getFeatureNameOrNull(feature, expressionHelper);
            if (name == null) {
                return null;
            }
            result.add(new FeatureInfo(name, resolveType(feature, sourceMap), feature));
        }
        return result;
    }

    private static List<FeatureInfo> getCopiedFeatureInfos(EClass sourceClass, Map<EClass, Set<Query>> sourceMap) {
        var result = new ArrayList<FeatureInfo>();
        for (var feature : sourceClass.getEStructuralFeatures()) {
            FeatureType type = null;
            if (feature instanceof EAttribute attribute) {
                type = new AttributeType(attribute.getEAttributeType());
            } else if (feature instanceof EReference reference) {
                type = resolveReferenceType(reference.getEReferenceType(), sourceMap);
            }
            result.add(new FeatureInfo(feature.getName(), type, null));
        }
        return result;
    }

    /**
     * Determines the type of the given feature like {@link tools.vitruv.neojoin.aqr.AQRBuilder} does.
     *
     * @return the type or {@code null} if it cannot be determined (e.g. because of other errors)
     */
    private @Nullable FeatureType resolveType(Feature feature, Map<EClass, Set<Query>> sourceMap) {
        if (feature.getType() != null) {
            return resolveExplicitType(feature);
        }
        if (feature instanceof ConcreteFeature concreteFeature && concreteFeature.getExpression() != null) {
            return resolveInferredType(concreteFeature, sourceMap);
        }
        return null;
    }

    private static @Nullable FeatureType resolveExplicitType(Feature feature) {
        var explicitType = feature.getType();
        if (explicitType == null || explicitType.eIsProxy()) {
            return null;
        } else if (explicitType instanceof EDataType dataType) {
            return new AttributeType(dataType);
        } else if (explicitType instanceof Query query) {
            return new QueryReferenceType(query);
        }
        return null;
    }

    private @Nullable FeatureType resolveInferredType(
        ConcreteFeature feature,
        Map<EClass, Set<Query>> sourceMap
    ) {
        if (feature.getSubQuery() != null) {
            return new QueryReferenceType(feature.getSubQuery());
        }

        try {
            var inferredType = expressionHelper.inferEType(feature.getExpression());
            if (inferredType == null || inferredType.classifier() == null) {
                return null;
            }

            if (inferredType.classifier() instanceof EDataType inferredDataType) {
                if (feature.getOp() == FeatureOp.COPY) {
                    var copied = expressionHelper.getFeatureOrNull(feature.getExpression());
                    if (copied != null && copied.getEType() instanceof EDataType copiedDataType) {
                        return new AttributeType(copiedDataType);
                    }
                }
                return new AttributeType(inferredDataType);
            } else if (inferredType.classifier() instanceof EClass inferredClass) {
                return resolveReferenceType(inferredClass, sourceMap);
            }
        } catch (TypeResolutionException e) {
            // ignore: will be handled by type checking
        }
        return null;
    }

    private static @Nullable ReferenceType resolveReferenceType(
        EClass sourceClass,
        Map<EClass, Set<Query>> sourceMap
    ) {
        var candidates = sourceMap.get(sourceClass);
        if (candidates == null || candidates.isEmpty()) {
            return new ImplicitReferenceType(sourceClass);
        } else if (candidates.size() == 1) {
            return new QueryReferenceType(candidates.iterator().next());
        } else {
            return null; // ambiguous: reported elsewhere
        }
    }

}
