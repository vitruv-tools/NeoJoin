package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XBooleanLiteral;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XStringLiteral;

import tools.vitruv.neojoin.expression_parser.model.predicate_expression.ComparisonOperator;
import tools.vitruv.neojoin.expression_parser.model.predicate_expression.ConstantValue;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;

import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PredicateExpressionUtils {
    private static final Map<String, ComparisonOperator> OPERATOR_MAP =
            Map.ofEntries(
                    Map.entry("operator_equals", ComparisonOperator.Equals),
                    Map.entry("operator_notEquals", ComparisonOperator.NotEquals),
                    Map.entry("operator_lessThan", ComparisonOperator.LessThan),
                    Map.entry("operator_lessEqualsThan", ComparisonOperator.LessEquals),
                    Map.entry("operator_greaterThan", ComparisonOperator.GreaterThan),
                    Map.entry("operator_greaterEqualsThan", ComparisonOperator.GreaterEquals));

    /**
     * Extracts a ConstantPredicate out of the binary operation. The order of operations is expected
     * to be FIELD - COMPARISON_OPERATOR - CONSTANT_VALUE
     *
     * @throws UnsupportedReferenceExpressionException If the expression doesn't match the expected
     *     format
     */
    public static ConstantPredicate extractConstantPredicate(XBinaryOperation operation)
            throws UnsupportedReferenceExpressionException {
        // Left side should be a feature/field call
        final XExpression leftOperand = operation.getLeftOperand();
        final Optional<String> fieldSimpleName =
                Optional.ofNullable(leftOperand)
                        .flatMap(CastingUtils::asMemberFeatureCall)
                        .map(XAbstractFeatureCall::getFeature)
                        .flatMap(CastingUtils::asJvmField)
                        .map(JvmField::getSimpleName);
        if (fieldSimpleName.isEmpty()) {
            throw new UnsupportedReferenceExpressionException(
                    String.format(
                            "The left side of a filter expression must be a field, but was: %s",
                            leftOperand));
        }

        final Optional<ComparisonOperator> comparisonOperator = getComparisonOperator(operation);
        if (comparisonOperator.isEmpty()) {
            throw new UnsupportedReferenceExpressionException(
                    "The comparison operator is not supported");
        }

        // Right side should be a constant
        final XExpression rightOperand = operation.getRightOperand();
        final Optional<ConstantValue> constantValue = getConstantExpressionAsString(rightOperand);
        if (constantValue.isEmpty()) {
            throw new UnsupportedReferenceExpressionException(
                    String.format(
                            "The right side of a filter expression must be a constant, but was: %s",
                            rightOperand));
        }

        return new ConstantPredicate(
                fieldSimpleName.get(), comparisonOperator.get(), constantValue.get());
    }

    private static Optional<ComparisonOperator> getComparisonOperator(XBinaryOperation operation) {
        return Optional.ofNullable(operation)
                .map(XAbstractFeatureCall::getFeature)
                .flatMap(CastingUtils::asJvmOperation)
                .map(JvmOperation::getSimpleName)
                .map(OPERATOR_MAP::get);
    }

    private static Optional<ConstantValue> getConstantExpressionAsString(XExpression expression) {
        if (expression instanceof XNumberLiteral numberLiteral) {
            return Optional.of(ConstantValue.of(numberLiteral.getValue()));
        } else if (expression instanceof XStringLiteral stringLiteral) {
            return Optional.of(ConstantValue.String(stringLiteral.getValue()));
        } else if (expression instanceof XBooleanLiteral booleanLiteral) {
            return Optional.of(ConstantValue.Boolean(booleanLiteral.isIsTrue()));
        }
        return Optional.empty();
    }

    @Value
    public static class ConstantPredicate {
        String feature;
        ComparisonOperator operator;
        ConstantValue constantValue;
    }
}
