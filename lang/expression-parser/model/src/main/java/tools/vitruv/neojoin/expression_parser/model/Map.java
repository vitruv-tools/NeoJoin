package tools.vitruv.neojoin.expression_parser.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A Map represents mapping a parent object to some child along a <b>one-to-one</b> reference. It
 * contains information about the reference and the child type
 *
 * <p>An example expression may look like
 *
 * <pre>
 *     {@code someResult = car.axis.map(a -> a.axisInformation).toList()}
 * </pre>
 *
 * Here, {@code X.map(a -> a.axisInformation)} is a Map
 */
public class Map implements ReferenceOperator {
    @NonNull final FeatureInformation featureInformation;

    @Nullable ReferenceOperator followingOperator;

    public Map(FeatureInformation featureInformation) {
        this.featureInformation = featureInformation;
    }

    @Override
    public String toString() {
        final String stringRepresentation = "Map(" + featureInformation.getFeatureName() + ")";
        if (followingOperator == null) {
            return stringRepresentation;
        }

        return stringRepresentation + "->" + followingOperator;
    }

    public FeatureInformation getFeatureInformation() {
        return featureInformation;
    }

    public @Nullable ReferenceOperator getFollowingOperator() {
        return followingOperator;
    }

    public void setFollowingOperator(@Nullable ReferenceOperator followingOperator) {
        this.followingOperator = followingOperator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Map that)) {
            return false;
        }
        return Objects.equals(featureInformation, that.featureInformation) &&
                Objects.equals(followingOperator, that.followingOperator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(featureInformation, followingOperator);
    }
}
