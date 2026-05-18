package tools.vitruv.neojoin.expression_parser.model.predicate_expression;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ComparisonOperator {
    Equals("=="),
    NotEquals("!="),
    LessThan("<"),
    LessEquals("<="),
    GreaterThan(">"),
    GreaterEquals(">=");

    final String representation;
}
