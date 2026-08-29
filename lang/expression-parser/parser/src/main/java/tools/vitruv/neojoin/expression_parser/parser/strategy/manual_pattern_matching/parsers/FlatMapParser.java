package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

import tools.vitruv.neojoin.expression_parser.model.FeatureCall;
import tools.vitruv.neojoin.expression_parser.model.FlatMap;
import tools.vitruv.neojoin.expression_parser.model.Map;
import tools.vitruv.neojoin.expression_parser.model.MemberFeatureCall;
import tools.vitruv.neojoin.expression_parser.model.ReferenceFilter;
import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.PatternMatchingStrategy;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.BlockExpressionUtils;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.CastingUtils;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.JvmMemberCallUtils;

import java.util.Optional;

public class FlatMapParser implements ReferenceOperatorParser {
    private static final String FLAT_MAP_OPERATION_SIMPLE_NAME = "flatMap";

    public Optional<ReferenceOperator> parse(
            PatternMatchingStrategy strategy, XExpression expression)
            throws UnsupportedReferenceExpressionException {
        // Check that expression is flatMap and get single argument
        final Optional<XExpression> flatMapArgument =
                CastingUtils.asMemberFeatureCall(expression)
                        .filter(FlatMapParser::isFlatMapOperation)
                        .filter(JvmMemberCallUtils::hasExactlyOneMemberCallArgument)
                        .flatMap(JvmMemberCallUtils::getFirstArgument);
        if (flatMapArgument.isEmpty()) {
            return Optional.empty();
        }

        // Check that expression can be parsed
        final Optional<XExpression> flatMapArgumentExpression =
                flatMapArgument
                        .flatMap(CastingUtils::asClosure)
                        .map(XClosure::getExpression)
                        .flatMap(CastingUtils::asBlockExpression)
                        .filter(BlockExpressionUtils::hasExactlyOneExpression)
                        .flatMap(BlockExpressionUtils::getFirstExpression);
        if (flatMapArgumentExpression.isEmpty()) {
            return Optional.empty();
        }

        final ReferenceOperator flatMapArgumentOperator =
                strategy.parseReferenceOperator(flatMapArgumentExpression.get());
        if (!(flatMapArgumentOperator instanceof FeatureCall)) {
            throw new UnsupportedReferenceExpressionException(
                    "The first element of a flatMap expression must be a feature call",
                    flatMapArgumentExpression.get());
        }

        ReferenceOperator currentFlatMapArgumentOperator =
                flatMapArgumentOperator.getFollowingOperator();
        if (currentFlatMapArgumentOperator == null) {
            throw new UnsupportedReferenceExpressionException(
                    "The flatMap expression must contain more than a feature call",
                    flatMapArgumentExpression.get());
        }

        final ReferenceOperator operatorHead =
                extractFlatMapArgumentOperator(
                        currentFlatMapArgumentOperator, flatMapArgumentExpression.get());
        currentFlatMapArgumentOperator = currentFlatMapArgumentOperator.getFollowingOperator();

        ReferenceOperator lastOperator = operatorHead;
        while (currentFlatMapArgumentOperator != null) {
            final ReferenceOperator nextOperator =
                    extractFlatMapArgumentOperator(
                            currentFlatMapArgumentOperator, flatMapArgumentExpression.get());

            lastOperator.setFollowingOperator(nextOperator);
            lastOperator = nextOperator;
            currentFlatMapArgumentOperator = currentFlatMapArgumentOperator.getFollowingOperator();
        }

        return parseAndAppendFollowingExpressionOperators(strategy, expression, operatorHead);
    }

    private static ReferenceOperator extractFlatMapArgumentOperator(
            ReferenceOperator flatMapArgumentOperator, XExpression flatMapArgumentExpression)
            throws UnsupportedReferenceExpressionException {
        if (flatMapArgumentOperator instanceof MemberFeatureCall memberFeatureCall
                && memberFeatureCall.isCollection()) {
            return new FlatMap(memberFeatureCall.getFeatureInformation());
        } else if (flatMapArgumentOperator instanceof MemberFeatureCall memberFeatureCall
                && !memberFeatureCall.isCollection()) {
            return new Map(memberFeatureCall.getFeatureInformation());
        } else if (flatMapArgumentOperator instanceof Map mapCall) {
            return new Map(mapCall.getFeatureInformation());
        } else if (flatMapArgumentOperator instanceof FlatMap flatMapCall) {
            return new FlatMap(flatMapCall.getFeatureInformation());
        } else if (flatMapArgumentOperator instanceof ReferenceFilter filter) {
            return new ReferenceFilter(
                    filter.getFeature(), filter.getOperator(), filter.getConstantValue());
        }

        throw new UnsupportedReferenceExpressionException(
                "The flatMap expression is not supported", flatMapArgumentExpression);
    }

    private static boolean isFlatMapOperation(XMemberFeatureCall featureCall) {
        return Optional.ofNullable(featureCall)
                .map(XAbstractFeatureCall::getFeature)
                .flatMap(CastingUtils::asJvmOperation)
                .map(JvmIdentifiableElement::getSimpleName)
                .map(FLAT_MAP_OPERATION_SIMPLE_NAME::equals)
                .orElse(false);
    }
}
