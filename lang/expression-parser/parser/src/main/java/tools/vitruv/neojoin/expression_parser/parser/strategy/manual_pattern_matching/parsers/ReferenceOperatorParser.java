package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.jspecify.annotations.Nullable;

import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.PatternMatchingStrategy;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.CastingUtils;

import java.util.Optional;

public interface ReferenceOperatorParser {
    Optional<ReferenceOperator> parse(PatternMatchingStrategy strategy, XExpression expression)
            throws UnsupportedReferenceExpressionException;

    /** Returns the next call target if the expression is a {@code XMemberFeatureCall} */
    default Optional<XAbstractFeatureCall> findNextCallTarget(XExpression expression) {
        return CastingUtils.asMemberFeatureCall(expression)
                .map(XMemberFeatureCall::getMemberCallTarget)
                .flatMap(CastingUtils::asAbstractFeatureCall);
    }

    /**
     * Runs the Reference operator extraction on the following {@code memberCallTarget} and appends
     * it to the existing ReferenceOperator chain
     */
    default Optional<ReferenceOperator> parseAndAppendFollowingExpressionOperators(
            PatternMatchingStrategy strategy,
            XExpression currentExpression,
            @Nullable ReferenceOperator currentOperator)
            throws UnsupportedReferenceExpressionException {
        final Optional<XAbstractFeatureCall> nextMemberCallTarget =
                findNextCallTarget(currentExpression);
        if (nextMemberCallTarget.isPresent()) {
            // Parse the following expression
            final ReferenceOperator followingOperator =
                    strategy.parseReferenceOperator(nextMemberCallTarget.get());

            // As the Expression "AST" contains the expressions in reverse order, the current
            // expression should come after the expression we parsed afterward
            followingOperator.getLastOperatorInChain().setFollowingOperator(currentOperator);
            return Optional.of(followingOperator);
        }

        // If there is no following operator, we are at the end of the expression chain
        return Optional.ofNullable(currentOperator);
    }
}
