package tools.vitruv.neojoin.validation;

import com.google.inject.Inject;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.ast.ConcreteFeature;
import tools.vitruv.neojoin.ast.ConcreteMainQuery;
import tools.vitruv.neojoin.ast.MainQuery;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.AstUtils;
import tools.vitruv.neojoin.utils.FeatureInfo;
import tools.vitruv.neojoin.utils.FeatureInfoResolver;
import tools.vitruv.neojoin.utils.FeatureType;
import tools.vitruv.neojoin.utils.FeatureType.AmbiguousReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.AttributeType;
import tools.vitruv.neojoin.utils.FeatureType.EcoreReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.QueryReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.ReferenceType;
import tools.vitruv.neojoin.utils.InheritedFeatures;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates the inheritance relations between target classes (see {@code extends} in queries).
 * <p>
 * The checks mirror the invariants of {@link tools.vitruv.neojoin.aqr.AQRBuilder} (wiring of the
 * super classes,
 * application of inheritance, and determination of reference types) so that invalid queries are reported as issues
 * instead of failing with an {@code AQRInvariantViolatedException} during AQR construction. They also protect the
 * later stages, which rely on a well-formed class hierarchy:
 * {@link tools.vitruv.neojoin.generation.MetaModelGenerator} maps the super classes to
 * {@code eSuperTypes} (which must be acyclic and duplicate free), and the transformation resolves references to
 * instances of sub classes via the transitive super classes of the target classes and evaluates overriding
 * features via their overriding kind.
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
     * cyclic (invalid Ecore), and walking the hierarchy (e.g. collecting all transitive super
     * classes) would not terminate or would produce meaningless results.
     */
    @Check
    public void checkAcyclicInheritance(MainQuery query) {
        var superClasses = query.getSuperClasses();
        for (var i = 0; i < superClasses.size(); i++) {
            var superClass = superClasses.get(i);
            if (superClass.eIsProxy()) {
                continue; // unresolved: will be handled by linking
            }

            if (superClass == query) {
                error("A class cannot extend itself", query, AstPackage.Literals.MAIN_QUERY__SUPER_CLASSES, i);
            } else if (AstUtils.isSameOrSuperClassOf(query, superClass)) {
                error(
                    "Cyclic inheritance: '%s' is a sub class of '%s'".formatted(
                        AstUtils.getTargetName(superClass, expressionHelper),
                        AstUtils.getTargetName(query, expressionHelper)
                    ),
                    query,
                    AstPackage.Literals.MAIN_QUERY__SUPER_CLASSES,
                    i
                );
            }
        }
    }

    /**
     * Checks that a class lists each super class only once.
     * <p>
     * A duplicated super class would be added twice to the super classes of the target class in the AQR
     * and to the {@code eSuperTypes} of the generated class.
     */
    @Check
    public void checkDuplicatedSuperClasses(MainQuery query) {
        var superClasses = query.getSuperClasses();
        for (var i = 0; i < superClasses.size(); i++) {
            var superClass = superClasses.get(i);
            if (superClass.eIsProxy()) {
                continue; // unresolved: will be handled by linking
            }

            if (superClasses.stream().filter(other -> other == superClass).count() > 1) {
                error(
                    "Duplicated super class: " + AstUtils.getTargetName(superClass, expressionHelper),
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
     * The containment references of the root class are generated last (after inheritance was applied), so they
     * are not part of the features that the sub class has to override. The sub
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
     * Checks that a feature that is inherited from multiple super classes comes from the same declaration in all of
     * them.
     * <p>
     * A class overrides a feature that is provided by several super classes (diamond) only once, and
     * the AQR construction takes the type and modifiers of the overridden feature. This is only
     * well-defined if all super classes inherit the feature from the same declaration (for example, from a common
     * super class). Ecore allows a feature name only once per class, including inherited features, so independent
     * declarations with the same name conflict in the generated view type, even if they have the same type and
     * modifiers.
     */
    @Check
    public void checkConflictingInheritedFeatures(MainQuery query) {
        var inherited = getInheritedFeatures(query);
        for (var entry : inherited.superClassesByFeatureName().entrySet()) {
            var superClasses = entry.getValue();
            if (superClasses.size() > 1 && inherited.hasConflictingProviders(entry.getKey())) {
                var classNames = superClasses.stream()
                    .map(c -> AstUtils.getTargetName(c, expressionHelper))
                    .collect(Collectors.joining(", "));
                for (var superClass : superClasses) {
                    errorAtSuperClass(
                        ("Feature '%s' is inherited from multiple super classes "
                            + "that declare it independently: %s").formatted(entry.getKey(), classNames),
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
     * Without a declaration, the transformation would have no way to compute the value (it only evaluates
     * the overriding kind of an overriding feature). The AQR construction requires this ("Sub classes must override
     * all inherited features").
     */
    @Check
    public void checkInheritedFeaturesAreOverridden(MainQuery query) {
        var inherited = getInheritedFeatures(query);
        var own = getOwnFeatures(query);
        if (own == null) {
            return; // features cannot be determined: other errors will be reported
        }

        var ownNames = own.stream().map(FeatureInfo::name).collect(Collectors.toSet());
        inherited.bySuperClass().forEach((superClass, features) -> {
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
     * The AQR construction replaces the modifiers of an overriding feature by the modifiers of the overridden
     * feature, so modifiers written on the overriding feature have no effect.
     */
    @Check
    public void checkOverridingFeatureModifiers(MainQuery query) {
        var overriddenNames = getInheritedFeatures(query).superClassesByFeatureName().keySet();
        var own = getOwnFeatures(query);
        if (own == null) {
            return; // features cannot be determined: other errors will be reported
        }

        for (var feature : own) {
            if (overriddenNames.contains(feature.name())
                && feature.node() != null
                && !feature.node().getModifiers().isEmpty()) {
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
     * a sub class (invariants of the AQR construction). Otherwise, the generated view type would be
     * invalid, and values could not be assigned to the inherited feature during the transformation.
     * Features that are inherited from conflicting features of multiple super classes are skipped (see
     * {@link #checkConflictingInheritedFeatures}); a feature that is inherited from the same declaration
     * through several super classes is overridden once.
     */
    @Check
    public void checkOverridingFeatureTypes(MainQuery query) {
        var overriddenByName = getInheritedFeatures(query).overriddenFeatures();
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
     * The modifiers of an overriding feature are ignored (the AQR construction takes them from the overridden
     * feature), so {@link FeatureModifierValidator#checkMultiplicity} compares the expression only with
     * the modifiers written on the overriding feature. A single value for a multi-valued feature (or the other way
     * around) would make the transformation cast the value to the wrong kind. Nothing is reported if the
     * overriding feature writes a multiplicity with the same cardinality as the overridden one, because
     * {@code FeatureModifierValidator} already reports the same error.
     */
    @Check
    public void checkOverridingFeatureMultiplicity(MainQuery query) {
        var overriddenByName = getInheritedFeatures(query).overriddenFeatures();
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

            var hasExplicitType = concreteFeature.getType() != null;
            if (getOverrideViolation(
                feature.name(),
                feature.type(),
                hasExplicitType,
                overridden.right().type()
            ) != null) {
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
     * (see the AQR construction) and not on the modifiers written on the overriding feature. The check for other
     * classes is part of the {@link FeatureValidator}, which does not know the inherited modifiers.
     */
    @Check
    public void checkNullableExpressionForRequiredFeature(MainQuery query) {
        if (query.getSuperClasses().isEmpty()) {
            return;
        }

        var overriddenByName = getInheritedFeatures(query).overriddenFeatures();
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

    private InheritedFeatures getInheritedFeatures(MainQuery query) {
        return InheritedFeatures.of(query, new FeatureInfoResolver(AstUtils.getViewType(query), expressionHelper));
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
            return null; // reported by FeatureValidator
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
            // references to Ecore classes have no target class, so they only match references to the same Ecore class
            if (reference instanceof EcoreReferenceType && !(overriddenReference instanceof EcoreReferenceType)) {
                return "Reference '%s' to an Ecore class cannot override a reference to a target class".formatted(name);
            }
            if (!(reference instanceof EcoreReferenceType) && overriddenReference instanceof EcoreReferenceType) {
                return "Reference '%s' to a target class cannot override a reference to an Ecore class".formatted(name);
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
        return subType.equals(superType)
            || (subType instanceof QueryReferenceType sub
                && superType instanceof QueryReferenceType sup
                && AstUtils.isSameOrSuperClassOf(sup.query(), sub.query()));
    }

}
