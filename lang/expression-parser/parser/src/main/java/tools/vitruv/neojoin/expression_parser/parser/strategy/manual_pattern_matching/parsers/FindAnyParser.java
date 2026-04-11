package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

import tools.vitruv.neojoin.expression_parser.model.FindAny;
import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.PatternMatchingStrategy;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.BlockExpressionUtils;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.CastingUtils;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.JvmMemberCallUtils;

import java.util.Optional;
import java.util.Set;

public class FindAnyParser implements ReferenceOperatorParser {
    private static final String FIND_FIRST_OPERATION_SIMPLE_NAME = "findFirst";
    private static final String FIND_LAST_OPERATION_SIMPLE_NAME = "findLast";
    private static final Set<String> FIND_ANY_OPERATION_SIMPLE_NAMES =
            Set.of(FIND_FIRST_OPERATION_SIMPLE_NAME, FIND_LAST_OPERATION_SIMPLE_NAME);

    public Optional<ReferenceOperator> parse(
            PatternMatchingStrategy strategy, XExpression expression)
            throws UnsupportedReferenceExpressionException {
        boolean isFindAnyWithoutArguments =
                Optional.ofNullable(expression)
                        .flatMap(CastingUtils::asMemberFeatureCall)
                        .filter(FindAnyParser::isFindAnyOperation)
                        .filter(JvmMemberCallUtils::hasExactlyOneMemberCallArgument)
                        .flatMap(JvmMemberCallUtils::getFirstArgument)
                        .flatMap(CastingUtils::asClosure)
                        .map(XClosure::getExpression)
                        .flatMap(CastingUtils::asBlockExpression)
                        .filter(BlockExpressionUtils::hasNoExpressions)
                        .isPresent();
        if (!isFindAnyWithoutArguments) {
            return Optional.empty();
        }

        return parseAndAppendFollowingExpressionOperators(strategy, expression, new FindAny());
    }

    private static boolean isFindAnyOperation(XMemberFeatureCall featureCall) {
        return Optional.ofNullable(featureCall)
                .map(XAbstractFeatureCall::getFeature)
                .flatMap(CastingUtils::asJvmOperation)
                .map(JvmOperation::getSimpleName)
                .map(FIND_ANY_OPERATION_SIMPLE_NAMES::contains)
                .orElse(false);
    }
}
