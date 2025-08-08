package tools.vitruv.neojoin.aqr;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Class within the target model.
 *
 * @implNote This class is not a record because it requires identity and a record's
 * {@link Record#equals(Object) equals()} and {@link Record#hashCode() hashCode()}
 * explicitly forbid it.
 * Identity is required on a conceptional level, because a target class should not be equal to
 * any other target class even if all properties are equal (e.g. by creating an exact copy of a query).
 * Additionally, it helps to prevent errors with self references in the {@link #features() features}
 * that would lead to endless recursion in {@link #equals(Object) equals()} and {@link #hashCode()}.
 */
@SuppressWarnings("ClassCanBeRecord")
public final class AQRTargetClass {

    private final String name;
    private final @Nullable AQRSource source;
    private final List<AQRFeature> features;

    /**
     * @param name     name of the target class
     * @param source   source of the target class
     * @param features features of the target class
     */
    public AQRTargetClass(
        String name,
        @Nullable AQRSource source,
        List<AQRFeature> features
    ) {
        this.name = name;
        this.source = source;
        this.features = features;
    }

    public String name() {
        return name;
    }

    public @Nullable AQRSource source() {
        return source;
    }

    public List<AQRFeature> features() {
        return features;
    }

    @Override
    public String toString() {
        return "TargetClass[name=%s, source=%s, features=%s]".formatted(
            name,
            source,
            features
        );
    }

}
