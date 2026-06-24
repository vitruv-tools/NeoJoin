package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

import tools.vitruv.neojoin.expression_parser.model.FeatureCall;
import tools.vitruv.neojoin.expression_parser.model.Map;
import tools.vitruv.neojoin.expression_parser.model.MemberFeatureCall;
import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.PatternMatchingStrategy;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.BlockExpressionUtils;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.CastingUtils;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.JvmMemberCallUtils;

import java.util.Optional;

public class MapParser implements ReferenceOperatorParser {
    private static final String MAP_OPERATION_SIMPLE_NAME = "map";

    public Optional<ReferenceOperator> parse(
            PatternMatchingStrategy strategy, XExpression expression)
            throws UnsupportedReferenceExpressionException {
        // Check that expression is map and get single argument
        final Optional<XExpression> mapArgument =
                CastingUtils.asMemberFeatureCall(expression)
                        .filter(MapParser::isMapOperation)
                        .filter(JvmMemberCallUtils::hasExactlyOneMemberCallArgument)
                        .flatMap(JvmMemberCallUtils::getFirstArgument);
        if (mapArgument.isEmpty()) {
            return Optional.empty();
        }

        // Check that expression can be parsed
        final Optional<XExpression> mapArgumentExpression =
                mapArgument
                        .flatMap(CastingUtils::asClosure)
                        .map(XClosure::getExpression)
                        .flatMap(CastingUtils::asBlockExpression)
                        .filter(BlockExpressionUtils::hasExactlyOneExpression)
                        .flatMap(BlockExpressionUtils::getFirstExpression);
        if (mapArgumentExpression.isEmpty()) {
            return Optional.empty();
        }

        final ReferenceOperator mapArgumentOperator =
                strategy.parseReferenceOperator(mapArgumentExpression.get());
        if (!(mapArgumentOperator instanceof FeatureCall)) {
            throw new UnsupportedReferenceExpressionException(
                    "The first element of a map expression must be a feature call",
                    mapArgumentExpression.get());
        }

        ReferenceOperator currentMapArgumentOperator = mapArgumentOperator.getFollowingOperator();
        if (currentMapArgumentOperator == null) {
            throw new UnsupportedReferenceExpressionException(
                    "The map expression must contain more than a feature call",
                    mapArgumentExpression.get());
        }

        // Loop through each parsed ReferenceOperators inside the map arguments and map/extract them
        // to the top level ReferenceOperator chain
        final ReferenceOperator extractedOperatorHead =
                extractMapArgumentOperator(currentMapArgumentOperator, mapArgumentExpression.get());
        currentMapArgumentOperator = currentMapArgumentOperator.getFollowingOperator();

        ReferenceOperator currentExtractedOperator = extractedOperatorHead;
        while (currentMapArgumentOperator != null) {
            final ReferenceOperator nextOperator =
                    extractMapArgumentOperator(
                            currentMapArgumentOperator, mapArgumentExpression.get());

            currentExtractedOperator.setFollowingOperator(nextOperator);
            currentExtractedOperator = nextOperator;
            currentMapArgumentOperator = currentMapArgumentOperator.getFollowingOperator();
        }

        return parseAndAppendFollowingExpressionOperators(
                strategy, expression, extractedOperatorHead);
    }

    private static ReferenceOperator extractMapArgumentOperator(
            ReferenceOperator mapArgumentOperator, XExpression mapArgumentExpression)
            throws UnsupportedReferenceExpressionException {
        // Currently, only one-to-one references are supported inside the map expression, since we
        // cannot handle the FindAny operator inside nested expression currently. Therefore, all
        // operators that return a collection are not possible
        if (mapArgumentOperator instanceof MemberFeatureCall memberFeatureCall
                && !memberFeatureCall.isCollection()) {
            return new Map(memberFeatureCall.getFeatureInformation());
        }

        throw new UnsupportedReferenceExpressionException(
                "The map expression is not supported", mapArgumentExpression);
    }

    private static boolean isMapOperation(XMemberFeatureCall featureCall) {
        return Optional.ofNullable(featureCall)
                .map(XAbstractFeatureCall::getFeature)
                .flatMap(CastingUtils::asJvmOperation)
                .map(JvmIdentifiableElement::getSimpleName)
                .map(MAP_OPERATION_SIMPLE_NAME::equals)
                .orElse(false);
    }
}
