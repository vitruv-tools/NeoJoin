package tools.vitruv.neojoin.expression_parser.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import tools.vitruv.neojoin.expression_parser.model.predicate_expression.ComparisonOperator;
import tools.vitruv.neojoin.expression_parser.model.predicate_expression.ConstantValue;

import java.util.Objects;

/**
 * A ReferenceFilter represents a predicate for the previous ReferenceOperator. Only predicates that
 * compare a feature to some constant value are supported
 *
 * <p>An example expression may look like
 *
 * <pre>
 *     {@code someResult = car.axis.filter(a -> a.position == "front").toList()}
 * </pre>
 *
 * Here, {@code X.filter(a -> a.position == "front")} is a ReferenceFilter
 */
public class ReferenceFilter implements ReferenceOperator {
    @NonNull final String feature;
    @NonNull final ComparisonOperator operator;
    @NonNull final ConstantValue constantValue;

    @Nullable ReferenceOperator followingOperator;

    public ReferenceFilter(String feature, ComparisonOperator operator, ConstantValue constantValue) {
        this.feature = feature;
        this.operator = operator;
        this.constantValue = constantValue;
    }

    @Override
    public String toString() {
        final String stringRepresentation =
                "ReferenceFilter("
                        + feature
                        + " "
                        + operator.getRepresentation()
                        + " "
                        + constantValue
                        + ")";
        if (followingOperator == null) {
            return stringRepresentation;
        }

        return stringRepresentation + "->" + followingOperator;
    }

    public String getFeature() {
        return feature;
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    public ConstantValue getConstantValue() {
        return constantValue;
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
        if (!(o instanceof ReferenceFilter that)) {
            return false;
        }
        return Objects.equals(feature, that.feature) &&
                Objects.equals(operator, that.operator) &&
                Objects.equals(constantValue, that.constantValue) &&
                Objects.equals(followingOperator, that.followingOperator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feature, operator, constantValue, followingOperator);
    }
}
