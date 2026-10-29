package tools.vitruv.neojoin.utils;

import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.aqr.AQRFeature;
import tools.vitruv.neojoin.ast.Feature;

import java.util.Objects;

/**
 * A feature of a target class as determined from the AST, see {@link FeatureInfoResolver}.
 *
 * @param name    name of the feature
 * @param type    type of the feature or {@code null} if it could not be determined
 * @param node    AST node of the feature or {@code null} for implicitly copied features
 * @param options effective modifiers of the feature (for an overriding feature the ones of the overridden feature) or
 *                {@code null} if they could not be determined
 */
public record FeatureInfo(
    String name,
    @Nullable FeatureType type,
    @Nullable Feature node,
    AQRFeature.@Nullable Options options
) {

    /**
     * Whether both features have the same type and the same modifiers. In contrast to {@link #equals}, the name and
     * the AST node are not compared. Features with unknown type or modifiers are only the same as features that have
     * the same unknown values.
     */
    public boolean hasSameTypeAndModifiers(FeatureInfo other) {
        return Objects.equals(type, other.type) && Objects.equals(options, other.options);
    }

}
