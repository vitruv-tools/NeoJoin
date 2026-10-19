package tools.vitruv.neojoin.expression_parser.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A MemberFeatureCall represents a field/reference access of a parent class. It contains
 * information about the type and reference name. The reference can have an upper and/or lower bound
 *
 * <p>An example expression may look like
 *
 * <pre>
 *     {@code someResult = car.axis.flatMap(a -> a.wheels).toList()}
 * </pre>
 *
 * Here, {@code X.axis} is a MemberFeatureCall
 */
public class MemberFeatureCall implements ReferenceOperator {
    @NonNull final FeatureInformation featureInformation;
    final boolean isCollection;

    @Nullable ReferenceOperator followingOperator;

    public MemberFeatureCall(FeatureInformation featureInformation, boolean isCollection) {
        this.featureInformation = featureInformation;
        this.isCollection = isCollection;
    }

    @Override
    public String toString() {
        final String stringRepresentation = "MemberFeatureCall(" + featureInformation.getFeatureName() + ")";
        if (followingOperator == null) {
            return stringRepresentation;
        }

        return stringRepresentation + "->" + followingOperator;
    }

    public FeatureInformation getFeatureInformation() {
        return featureInformation;
    }

    public boolean isCollection() {
        return isCollection;
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
        if (!(o instanceof MemberFeatureCall that)) {
            return false;
        }
        return Objects.equals(featureInformation, that.featureInformation) &&
                isCollection == that.isCollection &&
                Objects.equals(followingOperator, that.followingOperator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(featureInformation, isCollection, followingOperator);
    }
}
