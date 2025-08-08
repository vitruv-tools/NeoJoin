package tools.vitruv.neojoin.aqr;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class AQRFeatureOptionsAssertions extends AbstractAssert<AQRFeatureOptionsAssertions, AQRFeature.Options> {

    public enum Mod {
        Changeable(AQRFeature.Options::isChangeable, true),
        Volatile(AQRFeature.Options::isVolatile, false),
        Transient(AQRFeature.Options::isTransient, false),
        Unsettable(AQRFeature.Options::isUnsettable, false),
        Derived(AQRFeature.Options::isDerived, false),
        Unique(AQRFeature.Options::isUnique, true),
        Ordered(AQRFeature.Options::isOrdered, true),
        Containment(AQRFeature.Options::isContainment, false),
        Id(AQRFeature.Options::isID, false);

        final Function<AQRFeature.Options, Boolean> getter;
        final boolean defaultValue;

        Mod(Function<AQRFeature.Options, Boolean> getter, boolean defaultValue) {
            this.getter = getter;
            this.defaultValue = defaultValue;
        }
    }

    private final Set<Mod> seenModifiers = new HashSet<>();

    protected AQRFeatureOptionsAssertions(AQRFeature.Options actual) {
        super(actual, AQRFeatureOptionsAssertions.class);
    }

    public static AQRFeatureOptionsAssertions assertThat(AQRFeature.Options actual) {
        return new AQRFeatureOptionsAssertions(actual);
    }

    private AQRFeatureOptionsAssertions checkModifier(Mod mod, boolean expectedValue) {
        Assertions.assertThat(mod.getter.apply(actual))
            .as("Expected modifier '%s' to be %s", mod.name(), expectedValue)
            .isEqualTo(expectedValue);
        seenModifiers.add(mod);
        return this;
    }

    public AQRFeatureOptionsAssertions is(Mod mod) {
        return checkModifier(mod, true);
    }

    public AQRFeatureOptionsAssertions isNot(Mod mod) {
        return checkModifier(mod, false);
    }

    public AQRFeatureOptionsAssertions hasBounds(int lower, int upper) {
        Assertions.assertThat(actual.lowerBound())
            .as("Expected lower bound to be %d, but was %d instead", lower, actual.lowerBound())
            .isEqualTo(lower);
        Assertions.assertThat(actual.upperBound())
            .as("Expected upper bound to be %d, but was %d instead", upper, actual.upperBound())
            .isEqualTo(upper);
        return this;
    }

    public AQRFeatureOptionsAssertions otherModifiersHaveDefaultValue() {
        for (var mod : Mod.values()) {
            if (!seenModifiers.contains(mod)) {
                Assertions.assertThat(mod.getter.apply(actual))
                    .as("Expected modifier '%s' to be default (%s)", mod.name(), mod.defaultValue)
                    .isEqualTo(mod.defaultValue);
            }
        }
        return this;
    }

}
