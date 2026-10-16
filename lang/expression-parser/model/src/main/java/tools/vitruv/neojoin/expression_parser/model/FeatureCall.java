package tools.vitruv.neojoin.expression_parser.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;

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
@Data
@RequiredArgsConstructor
public class FeatureCall implements ReferenceOperator {
    @Nullable final String identifier;
    @Nullable final String simpleName;

    @Nullable ReferenceOperator followingOperator;

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
}
