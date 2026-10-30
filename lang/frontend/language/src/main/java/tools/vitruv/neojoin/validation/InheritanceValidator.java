package tools.vitruv.neojoin.validation;

import com.google.inject.Inject;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.aqr.AQRFeature;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.ast.ConcreteFeature;
import tools.vitruv.neojoin.ast.ConcreteMainQuery;
import tools.vitruv.neojoin.ast.Feature;
import tools.vitruv.neojoin.ast.MainQuery;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.AstUtils;
import tools.vitruv.neojoin.utils.FeatureInfo;
import tools.vitruv.neojoin.utils.FeatureInfoResolver;
import tools.vitruv.neojoin.utils.Pair;
import tools.vitruv.neojoin.utils.FeatureType;
import tools.vitruv.neojoin.utils.FeatureType.AmbiguousReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.AttributeType;
import tools.vitruv.neojoin.utils.FeatureType.QueryReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.ReferenceType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * {@code AQRTargetClass.allSuperClasses()} (see {@code TargetMap.get(...)} in the EMF backend) and
 * evaluates overriding features via {@code AQRFeature.Kind.Override} ({@code Transformator.evaluateFeature}).
 * <p>
 * Note that the rules only handle inheritance. The validation of features and types in general is done by
 * {@link FeatureValidator} and {@link FeatureModifierValidator}.
 */
public class InheritanceValidator extends ComposableValidator {

    @Inject
    private ExpressionHelper expressionHelper;

    // ---- super class list

    /**
     * Checks that a class does not (transitively) extend itself.
     * <p>
     * A class that extends itself has no finite set of super classes: the generated {@code eSuperTypes} would be
     * cyclic (invalid Ecore), and walking the hierarchy ({@code AQRTargetClass.allSuperClasses()},
     * {@code AstUtils.isSameOrSuperClassOf}) would not terminate or would produce meaningless results.
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
            } else if (AstUtils.isSameOrSuperClassOf(query, superQuery)) {
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
     * Checks that a feature that is inherited from multiple super classes has the same type and modifiers in all of
     * them.
     * <p>
     * A class overrides a feature that is provided by several super classes (diamond) only once, and
     * {@code AQRBuilder.applyInheritance} takes the type and modifiers of the overridden feature. This is only
     * well-defined if the provided features are equivalent (same name, type and effective modifiers). Otherwise, the
     * overriding feature cannot be assigned through all super classes in the generated view type and during the
     * transformation.
     */
    @Check
    public void checkConflictingInheritedFeatures(MainQuery query) {
        var inherited = getInheritedFeatures(query);
        for (var entry : getSuperClassesByFeatureName(inherited).entrySet()) {
            var superClasses = entry.getValue();
            if (superClasses.size() > 1 && hasConflictingProviders(entry.getKey(), superClasses, inherited)) {
                var classNames = superClasses.stream()
                    .map(c -> AstUtils.getTargetName(c, expressionHelper))
                    .collect(Collectors.joining(", "));
                for (var superClass : superClasses) {
                    errorAtSuperClass(
                        "Feature '%s' is inherited from multiple super classes with different types or modifiers: %s".formatted(entry.getKey(), classNames),
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
     * Features that are inherited from conflicting features of multiple super classes are skipped (see
     * {@link #checkConflictingInheritedFeatures}); equivalent ones are overridden once and the first provider
     * is the overridden feature.
     */
    @Check
    public void checkOverridingFeatureTypes(MainQuery query) {
        var overriddenByName = getOverriddenFeatures(query);
        var own = getOwnFeatures(query);
        if (own == null) {
            return; // features cannot be determined: other errors will be reported
        }

        for (var feature : own) {
            var overriddenEntry = overriddenByName.get(feature.name());
            if (overriddenEntry == null) {
                continue; // does not override or reported by checkConflictingInheritedFeatures
            }

            var superClass = overriddenEntry.left();
            var overridden = overriddenEntry.right();
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

    /**
     * Checks that the expression of an overriding feature has the multiplicity of the overridden feature.
     * <p>
     * The modifiers of an overriding feature are ignored ({@code AQRBuilder.applyInheritance} takes them from the
     * overridden feature), so {@link FeatureModifierValidator#checkMultiplicity} compares the expression only with
     * the modifiers written on the overriding feature. A single value for a multi-valued feature (or the other way
     * around) would make {@code Transformator.cast} cast the value to the wrong kind. Nothing is reported if the
     * overriding feature writes a multiplicity with the same cardinality as the overridden one, because
     * {@code FeatureModifierValidator} already reports the same error.
     */
    @Check
    public void checkOverridingFeatureMultiplicity(MainQuery query) {
        var overriddenByName = getOverriddenFeatures(query);
        var own = getOwnFeatures(query);
        if (own == null) {
            return; // features cannot be determined: other errors will be reported
        }

        for (var feature : own) {
            var overridden = overriddenByName.get(feature.name());
            if (overridden == null
                || overridden.right().options() == null
                || !(feature.node() instanceof ConcreteFeature concreteFeature)) {
                continue;
            }

            var hasExplicitType = feature.node() != null && feature.node().getType() != null;
            if (getOverrideViolation(feature.name(), feature.type(), hasExplicitType, overridden.right().type()) != null) {
                continue; // incompatible override, reported by checkOverridingFeatureTypes
            }

            var inheritedIsMany = overridden.right().options().isMany();
            var ownMultiplicity = AstUtils.findMultiplicityExpression(concreteFeature);
            if (ownMultiplicity != null && AstUtils.isManyMultiplicity(ownMultiplicity) == inheritedIsMany) {
                continue; // reported by FeatureModifierValidator
            }

            var inferredType = AstUtils.inferTypeOrNull(concreteFeature.getExpression(), expressionHelper);
            if (inferredType == null) {
                continue; // will be handled by type checking
            }
            if (inheritedIsMany && !inferredType.isMany()) {
                error(
                    "Cannot assign a single value to a multi-valued feature",
                    concreteFeature,
                    AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
                );
            } else if (!inheritedIsMany && inferredType.isMany()) {
                error(
                    "Cannot assign multiple values to a single-valued feature",
                    concreteFeature,
                    AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
                );
            }
        }
    }

    /**
     * Warns if a nullable expression ({@code ?.}) initializes a required feature of a class with super classes.
     * <p>
     * An overriding feature has the modifiers of the overridden feature, so whether it is required depends on those
     * ({@code AQRBuilder.applyInheritance}) and not on the modifiers written on the overriding feature. The check for
     * other classes is {@code FeatureValidator.checkFeatureNullability}, which does not know the inherited modifiers.
     */
    @Check
    public void checkNullableExpressionForRequiredFeature(MainQuery query) {
        if (query.getSuperClasses().isEmpty()) {
            return;
        }

        var overriddenByName = getOverriddenFeatures(query);
        var own = getOwnFeatures(query);
        if (own == null) {
            return; // features cannot be determined: other errors will be reported
        }

        for (var feature : own) {
            if (!(feature.node() instanceof ConcreteFeature concreteFeature)
                || !(concreteFeature.getExpression() instanceof XMemberFeatureCall featureCall)
                || !featureCall.isNullSafe()) {
                continue;
            }

            var overridden = overriddenByName.get(feature.name());
            var effectiveOptions = overridden != null ? overridden.right().options() : feature.options();
            if (effectiveOptions != null && effectiveOptions.isRequired()) {
                warning(
                    "Nullable expression used to initialize non-nullable feature",
                    concreteFeature,
                    AstPackage.Literals.CONCRETE_FEATURE__EXPRESSION
                );
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

    private static boolean hasCyclicSuperClasses(MainQuery query) {
        return query.getSuperClasses().stream().anyMatch(superClass ->
            superClass instanceof Query superQuery
                && !superQuery.eIsProxy()
                && (superQuery == query || AstUtils.isSameOrSuperClassOf(query, superQuery))
        );
    }

    private static FeatureInfo findFeature(List<FeatureInfo> features, String name) {
        return features.stream().filter(feature -> feature.name().equals(name)).findFirst().orElseThrow();
    }

    /**
     * Returns whether the features that the given super classes provide under the same name differ in type or
     * effective modifiers. Unknown types or modifiers are not a conflict (reported elsewhere).
     */
    private static boolean hasConflictingProviders(
        String name,
        List<Query> superClasses,
        Map<Query, List<FeatureInfo>> inherited
    ) {
        var first = findFeature(inherited.get(superClasses.get(0)), name);
        return superClasses.stream()
            .skip(1)
            .map(superClass -> findFeature(inherited.get(superClass), name))
            .anyMatch(other -> !isEquivalent(first, other));
    }

    private static boolean isEquivalent(FeatureInfo first, FeatureInfo second) {
        if (first.type() == null || second.type() == null || first.options() == null || second.options() == null) {
            return true; // unknown: reported elsewhere
        }
        return first.hasSameTypeAndModifiers(second);
    }

    /**
     * Returns the overridden feature by name of the feature, together with the super class that provides it. If several super classes provide equivalent features,
     * the first provider is the overridden feature. Features with conflicting providers are left out (reported by
     * {@link #checkConflictingInheritedFeatures}).
     */
    private Map<String, Pair<Query, FeatureInfo>> getOverriddenFeatures(MainQuery query) {
        var inherited = getInheritedFeatures(query);
        var result = new LinkedHashMap<String, Pair<Query, FeatureInfo>>();
        getSuperClassesByFeatureName(inherited).forEach((name, superClasses) -> {
            if (!hasConflictingProviders(name, superClasses, inherited)) {
                var superClass = superClasses.get(0);
                result.put(name, new Pair<>(superClass, findFeature(inherited.get(superClass), name)));
            }
        });
        return result;
    }

    /**
     * Returns the features of the super classes by super class (with the effective type and modifiers, see
     * {@link #getEffectiveFeatureInfos}). Super classes whose features cannot be determined are left out. The result is empty if the class has no super classes or if the inheritance is cyclic (reported by
     * {@link #checkAcyclicInheritance}).
     */
    private Map<Query, List<FeatureInfo>> getInheritedFeatures(MainQuery query) {
        var superClasses = AstUtils.getResolvedSuperClasses(query);
        if (superClasses.isEmpty() || hasCyclicSuperClasses(query)) {
            return Map.of();
        }

        var featureInfoResolver = new FeatureInfoResolver(AstUtils.getViewType(query), expressionHelper);
        var inherited = new LinkedHashMap<Query, List<FeatureInfo>>();
        for (var superClass : superClasses) {
            var features = featureInfoResolver.getEffectiveFeatureInfos(superClass);
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
        return new FeatureInfoResolver(AstUtils.getViewType(query), expressionHelper).getFeatureInfos(query);
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
        if (overriding instanceof AmbiguousReferenceType || overridden instanceof AmbiguousReferenceType) {
            return null; // reported by FeatureValidator.checkAmbiguousImplicitFeatureTypes
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
            if (!isSameOrSubReferenceType(reference, overriddenReference)) {
                return "Value of overriding feature '%s' cannot be assigned to the type of the overridden feature"
                    .formatted(name);
            }
        }
        return null;
    }

    private static boolean isSameOrSubReferenceType(ReferenceType subType, ReferenceType superType) {
        return subType.equals(superType) || (
            subType instanceof QueryReferenceType sub
                && superType instanceof QueryReferenceType sup
                && AstUtils.isSameOrSuperClassOf(sup.query(), sub.query())
        );
    }

}
