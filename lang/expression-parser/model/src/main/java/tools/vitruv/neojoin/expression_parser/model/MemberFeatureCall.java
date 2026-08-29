package tools.vitruv.neojoin.expression_parser.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
@Data
@RequiredArgsConstructor
public class MemberFeatureCall implements ReferenceOperator {
    @NonNull final FeatureInformation featureInformation;
    final boolean isCollection;

    @Nullable ReferenceOperator followingOperator;

    @Override
    public String toString() {
        final String stringRepresentation = "MemberFeatureCall(" + featureInformation.getFeatureName() + ")";
        if (followingOperator == null) {
            return stringRepresentation;
        }

        return stringRepresentation + "->" + followingOperator;
    }
}
