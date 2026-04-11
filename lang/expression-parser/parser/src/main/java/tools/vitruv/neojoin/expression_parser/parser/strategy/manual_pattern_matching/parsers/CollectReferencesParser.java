package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XExpression;

import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.PatternMatchingStrategy;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.CastingUtils;

import java.util.Optional;
import java.util.Set;

public class CollectReferencesParser implements ReferenceOperatorParser {
    private static final String TO_LIST_OPERATION_SIMPLE_NAME = "toList";
    private static final Set<String> COLLECT_OPERATIONS_SIMPLE_NAMES =
            Set.of(TO_LIST_OPERATION_SIMPLE_NAME);

    public Optional<ReferenceOperator> parse(
            PatternMatchingStrategy strategy, XExpression expression)
            throws UnsupportedReferenceExpressionException {
        final Optional<String> jvmOperationSimpleName =
                CastingUtils.asMemberFeatureCall(expression)
                        .map(XAbstractFeatureCall::getFeature)
                        .flatMap(CastingUtils::asJvmOperation)
                        .map(JvmIdentifiableElement::getSimpleName);
        if (jvmOperationSimpleName.isEmpty()) {
            return Optional.empty();
        }

        if (!COLLECT_OPERATIONS_SIMPLE_NAMES.contains(jvmOperationSimpleName.get())) {
            return Optional.empty();
        }

        return parseAndAppendFollowingExpressionOperators(strategy, expression, null);
    }
}
