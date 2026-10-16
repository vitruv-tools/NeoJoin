package tools.vitruv.neojoin.expression_parser.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
@Data
@RequiredArgsConstructor
public class Map implements ReferenceOperator {
    @NonNull final FeatureInformation featureInformation;

    @Nullable ReferenceOperator followingOperator;

    @Override
    public String toString() {
        final String stringRepresentation = "Map(" + featureInformation.getFeatureName() + ")";
        if (followingOperator == null) {
            return stringRepresentation;
        }

        return stringRepresentation + "->" + followingOperator;
    }
}
