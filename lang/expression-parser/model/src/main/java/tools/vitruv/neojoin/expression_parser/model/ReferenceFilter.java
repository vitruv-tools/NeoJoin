package tools.vitruv.neojoin.expression_parser.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import tools.vitruv.neojoin.expression_parser.model.predicate_expression.ComparisonOperator;
import tools.vitruv.neojoin.expression_parser.model.predicate_expression.ConstantValue;

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
@Data
@RequiredArgsConstructor
public class ReferenceFilter implements ReferenceOperator {
    @NonNull final String feature;
    @NonNull final ComparisonOperator operator;
    @NonNull final ConstantValue constantValue;

    @Nullable ReferenceOperator followingOperator;

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
}
