package tools.vitruv.neojoin.expression_parser.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A FlatMap represents mapping a parent object to some children along a <b>one-to-many</b>
 * reference and flattening the result. It contains information about the reference and the child type
 *
 * <p>An example expression may look like
 *
 * <pre>
 *     {@code someResult = car.axis.flatMap(a -> a.wheels).toList()}
 * </pre>
 *
 * Here, {@code X.flatMap(a -> a.wheels)} is a FlatMap
 */
public class FlatMap implements ReferenceOperator {
    @NonNull final FeatureInformation featureInformation;

    @Nullable ReferenceOperator followingOperator;

    public FlatMap(FeatureInformation featureInformation) {
        this.featureInformation = featureInformation;
    }

    @Override
    public String toString() {
        final String stringRepresentation = "FlatMap(" + featureInformation.getFeatureName() + ")";
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
        if (!(o instanceof FlatMap that)) {
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
