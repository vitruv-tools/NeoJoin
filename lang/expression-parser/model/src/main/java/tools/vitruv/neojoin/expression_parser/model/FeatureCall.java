package tools.vitruv.neojoin.expression_parser.model;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A FeatureCall represents a feature (e.g. a variable), stores information about the type and is
 * the first operation in a reference chain.
 *
 * <p>An example expression may look like
 *
 * <pre>
 *     {@code someResult = car.axis.flatMap(a -> a.wheels).toList()}
 * </pre>
 *
 * Here, {@code car} is a FeatureCall
 *
 * <p>A FeatureCall is also the first operation in a nested expression:
 *
 * <pre>
 *     {@code someResult = car.axis.flatMap(oneAxis -> oneAxis.wheels).toList()}
 * </pre>
 *
 * Here, {@code oneAxis} is also a FeatureCall
 */
public class FeatureCall implements ReferenceOperator {
    @Nullable final String identifier;
    @Nullable final String simpleName;

    @Nullable ReferenceOperator followingOperator;

    public FeatureCall(@Nullable String identifier, @Nullable String simpleName) {
        this.identifier = identifier;
        this.simpleName = simpleName;
    }

    public static FeatureCall empty() {
        return new FeatureCall(null, null);
    }

    @Override
    public String toString() {
        final String stringRepresentation = "FeatureCall(" + simpleName + ")";
        if (followingOperator == null) {
            return stringRepresentation;
        }

        return stringRepresentation + "->" + followingOperator;
    }

    public @Nullable String getIdentifier() {
        return identifier;
    }

    public @Nullable String getSimpleName() {
        return simpleName;
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
        if (!(o instanceof FeatureCall that)) {
            return false;
        }
        return Objects.equals(identifier, that.identifier) &&
                Objects.equals(simpleName, that.simpleName) &&
                Objects.equals(followingOperator, that.followingOperator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, simpleName, followingOperator);
    }
}
