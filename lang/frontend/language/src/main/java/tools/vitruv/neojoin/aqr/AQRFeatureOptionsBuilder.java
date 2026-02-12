package tools.vitruv.neojoin.aqr;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.ast.BooleanModifier;
import tools.vitruv.neojoin.ast.Feature;
import tools.vitruv.neojoin.ast.Modifier;
import tools.vitruv.neojoin.ast.MultiplicityBounds;
import tools.vitruv.neojoin.ast.MultiplicityExact;
import tools.vitruv.neojoin.ast.MultiplicityExpr;
import tools.vitruv.neojoin.ast.MultiplicityMany;
import tools.vitruv.neojoin.ast.MultiplicityManyAtLeast;
import tools.vitruv.neojoin.ast.MultiplicityManyRequired;
import tools.vitruv.neojoin.ast.MultiplicityOptional;
import tools.vitruv.neojoin.utils.Pair;

import java.util.List;
import java.util.function.Function;

import static tools.vitruv.neojoin.aqr.AQRInvariantViolatedException.invariant;
import static tools.vitruv.neojoin.utils.Assertions.fail;

/**
 * Infers the {@link AQRFeature.Options options} of a feature based on its modifiers.
 *
 * @see AQRBuilder
 */
public class AQRFeatureOptionsBuilder {

    private enum Mod {
        Changeable(true, "changeable", EStructuralFeature::isChangeable),
        Volatile(false, "volatile", EStructuralFeature::isVolatile),
        Transient(false, "transient", EStructuralFeature::isTransient),
        Unsettable(false, "unsettable", EStructuralFeature::isUnsettable),
        Derived(false, "derived", EStructuralFeature::isDerived),
        Unique(true, "unique", EStructuralFeature::isUnique),
        Ordered(true, "ordered", EStructuralFeature::isOrdered),
        Containment(false, "containment", f -> f instanceof EReference ref ? ref.isContainment() : null),
        Id(false, "id", f -> f instanceof EAttribute attr ? attr.isID() : null);

        final String keyword;
        final boolean defaultValue;
        final Function<EStructuralFeature, @Nullable Boolean> getter;

        Mod(
            boolean defaultValue,
            String keyword,
            Function<EStructuralFeature, @Nullable Boolean> getter
        ) {
            this.keyword = keyword;
            this.defaultValue = defaultValue;
            this.getter = getter;
        }
    }

    private final List<BooleanModifier> booleanModifiers;
    private final @Nullable Pair<Integer, Integer> multiplicity;

    @Nullable
    private final EStructuralFeature copyFrom;
    private final boolean inferredIsMany;

    public AQRFeatureOptionsBuilder(
        List<Modifier> modifiers,
        @Nullable EStructuralFeature copyFrom,
        boolean inferredIsMany
    ) {
        this.booleanModifiers = modifiers.stream()
            .filter(BooleanModifier.class::isInstance)
            .map(BooleanModifier.class::cast)
            .toList();
        this.multiplicity = modifiers.stream()
            .filter(MultiplicityExpr.class::isInstance)
            .map(MultiplicityExpr.class::cast)
            .map(AQRFeatureOptionsBuilder::normalizeMultiplicity)
            .findFirst().orElse(null);

        this.copyFrom = copyFrom;
        this.inferredIsMany = inferredIsMany;
    }

    public static AQRFeature.Options build(EStructuralFeature copyFrom) {
        // value for inferredIsMany is irrelevant here because copyFrom != null
        return new AQRFeatureOptionsBuilder(List.of(), copyFrom, false).build();
    }

    public static AQRFeature.Options build(
        Feature feature,
        @Nullable EStructuralFeature copyFrom,
        boolean inferredIsMany
    ) {
        var options = new AQRFeatureOptionsBuilder(feature.getModifiers(), copyFrom, inferredIsMany).build();
        invariant(options.isMany() == inferredIsMany);
        return options;
    }

    private AQRFeature.Options build() {
        var mult = getMultiplicity();
        //noinspection DataFlowIssue - false positive
        return new AQRFeature.Options(
            mult.left(),
            mult.right(),
            getBooleanModifierValue(Mod.Ordered),
            getBooleanModifierValue(Mod.Unique),
            getBooleanModifierValue(Mod.Changeable),
            getBooleanModifierValue(Mod.Transient),
            getBooleanModifierValue(Mod.Volatile),
            getBooleanModifierValue(Mod.Unsettable),
            getBooleanModifierValue(Mod.Derived),
            getBooleanModifierValue(Mod.Id),
            getBooleanModifierValue(Mod.Containment)
        );
    }

    private boolean getBooleanModifierValue(Mod modifier) {
        // explicitly specified using modifier
        var astModifier = booleanModifiers.stream()
            .filter(m -> m.getName().equals(modifier.keyword))
            .findFirst();
        if (astModifier.isPresent()) {
            return !astModifier.get().isNegated();
        }

        // copy from source
        if (copyFrom != null) {
            var value = modifier.getter.apply(copyFrom);
            if (value != null) {
                return value;
            }
        }

        return modifier.defaultValue;
    }

    private Pair<Integer, Integer> getMultiplicity() {
        if (multiplicity != null) {
            return multiplicity;
        }

        if (copyFrom != null) {
            return new Pair<>(copyFrom.getLowerBound(), copyFrom.getUpperBound());
        }

        return new Pair<>(0, inferredIsMany ? ETypedElement.UNBOUNDED_MULTIPLICITY : 1);
    }

    private static final int Unbounded = ETypedElement.UNBOUNDED_MULTIPLICITY;

    public static Pair<Integer, Integer> normalizeMultiplicity(MultiplicityExpr mult) {
        if (mult instanceof MultiplicityOptional) {
            return new Pair<>(0, 1); // [?]
        } else if (mult instanceof MultiplicityMany) {
            return new Pair<>(0, Unbounded); // [*]
        } else if (mult instanceof MultiplicityManyRequired) {
            return new Pair<>(1, Unbounded); // [+]
        } else if (mult instanceof MultiplicityExact exact) {
            return new Pair<>(exact.getExact(), exact.getExact()); // [x]
        } else if (mult instanceof MultiplicityBounds bounds) {
            return new Pair<>(bounds.getLowerBound(), bounds.getUpperBound()); // [x..y]
        } else if (mult instanceof MultiplicityManyAtLeast manyAtLeast) {
            return new Pair<>(manyAtLeast.getLowerBound(), Unbounded); // [x..*]
        } else {
            return fail();
        }
    }

}
