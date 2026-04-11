package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

import tools.vitruv.neojoin.expression_parser.model.ReferenceFilter;
import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.PatternMatchingStrategy;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.BlockExpressionUtils;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.CastingUtils;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.JvmMemberCallUtils;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.PredicateExpressionUtils;

import java.util.Optional;

public class FilterParser implements ReferenceOperatorParser {
    private static final String FILTER_MAP_OPERATION_SIMPLE_NAME = "filter";

    public Optional<ReferenceOperator> parse(
            PatternMatchingStrategy strategy, XExpression expression)
            throws UnsupportedReferenceExpressionException {
        final Optional<XBinaryOperation> binaryOperation =
                Optional.of(expression)
                        .flatMap(CastingUtils::asMemberFeatureCall)
                        .filter(FilterParser::isFilterOperation)
                        .filter(JvmMemberCallUtils::hasExactlyOneMemberCallArgument)
                        .flatMap(JvmMemberCallUtils::getFirstArgument)
                        .flatMap(CastingUtils::asClosure)
                        .map(XClosure::getExpression)
                        .flatMap(CastingUtils::asBlockExpression)
                        .filter(BlockExpressionUtils::hasExactlyOneExpression)
                        .flatMap(BlockExpressionUtils::getFirstExpression)
                        .flatMap(CastingUtils::asBinaryOperation);
        if (binaryOperation.isEmpty()) {
            return Optional.empty();
        }

        // Try to parse filter expression (throws if not possible)
        final PredicateExpressionUtils.ConstantPredicate constantFilterPredicate =
                PredicateExpressionUtils.extractConstantPredicate(binaryOperation.get());

        return parseAndAppendFollowingExpressionOperators(
                strategy,
                expression,
                new ReferenceFilter(
                        constantFilterPredicate.getFeature(),
                        constantFilterPredicate.getOperator(),
                        constantFilterPredicate.getConstantValue()));
    }

    private static boolean isFilterOperation(XMemberFeatureCall featureCall) {
        return Optional.ofNullable(featureCall)
                .map(XAbstractFeatureCall::getFeature)
                .flatMap(CastingUtils::asJvmOperation)
                .map(JvmIdentifiableElement::getSimpleName)
                .map(FILTER_MAP_OPERATION_SIMPLE_NAME::equals)
                .orElse(false);
    }
}
