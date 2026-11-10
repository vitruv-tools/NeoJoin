package tools.vitruv.neojoin.utils;

import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.ast.Feature;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.model.AQRFeature;

/**
 * A feature of a target class as determined from the AST, see {@link FeatureInfoResolver}.
 *
 * @param name           name of the feature
 * @param type           type of the feature or {@code null} if it could not be determined
 * @param node           AST node of the feature or {@code null} for implicitly copied features
 * @param options        effective modifiers of the feature (for an overriding feature the ones of the overridden
 *                       feature) or {@code null} if they could not be determined
 * @param declaringQuery the query whose target class declares the feature, for an overriding feature the one that
 *                       declares the overridden feature (two features of the same declaration are the same feature
 *                       in the generated view type); {@code null} for the features of an implicit class
 */
public record FeatureInfo(
    String name,
    @Nullable FeatureType type,
    @Nullable Feature node,
    AQRFeature.@Nullable Options options,
    @Nullable Query declaringQuery
) {

    /**
     * Whether both features have the same type and the same modifiers. In contrast to {@link #equals}, the name and
     * the AST node are not compared. A feature with unknown type or modifiers is treated like any other feature,
     * because the cause is reported elsewhere.
     */
    public boolean hasSameTypeAndModifiers(FeatureInfo other) {
        if (type == null || other.type == null || options == null || other.options == null) {
            return true;
        }
        return type.equals(other.type) && options.equals(other.options);
    }

}
