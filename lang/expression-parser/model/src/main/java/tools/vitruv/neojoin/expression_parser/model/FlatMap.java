package tools.vitruv.neojoin.expression_parser.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A FlatMap represents mapping a parent object to some children along a <b>one-to-many</b>
 * reference. It contains information about the reference and the child type
 *
 * <p>An example expression may look like
 *
 * <pre>
 *     {@code someResult = car.axis.flatMap(a -> a.wheels).toList()}
 * </pre>
 *
 * Here, {@code X.flatMap(a -> a.wheels)} is a FlatMap
 */
@Data
@RequiredArgsConstructor
public class FlatMap implements ReferenceOperator {
    @NonNull final FeatureInformation featureInformation;

    @Nullable ReferenceOperator followingOperator;

    @Override
    public String toString() {
        final String stringRepresentation = "FlatMap(" + featureInformation.getFeatureName() + ")";
        if (followingOperator == null) {
            return stringRepresentation;
        }

        return stringRepresentation + "->" + followingOperator;
    }
}
