package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching;

import org.eclipse.xtext.xbase.XExpression;
import org.jspecify.annotations.NonNull;

import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.PatternMatchingStrategy;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers.CollectReferencesParser;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers.FeatureCallParser;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers.FilterParser;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers.FindAnyParser;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers.FlatMapParser;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers.MapParser;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers.MemberFeatureCallParser;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers.ReferenceOperatorParser;

import java.util.List;

public class ManualPatternMatchingStrategy implements PatternMatchingStrategy {
    private static final List<ReferenceOperatorParser> PARSERS =
            List.of(
                    new FeatureCallParser(),
                    new MemberFeatureCallParser(),
                    new FilterParser(),
                    new CollectReferencesParser(),
                    new FlatMapParser(),
                    new MapParser(),
                    new FindAnyParser());

    @Override
    public @NonNull ReferenceOperator parseReferenceOperator(@NonNull XExpression expression)
            throws UnsupportedReferenceExpressionException {
        for (var parser : PARSERS) {
            final var result = parser.parse(this, expression);
            if (result.isPresent()) {
                return result.get();
            }
        }

        throw UnsupportedReferenceExpressionException.fromExpression(expression);
    }
}
