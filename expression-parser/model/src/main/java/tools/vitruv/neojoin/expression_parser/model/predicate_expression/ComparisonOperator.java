package tools.vitruv.neojoin.expression_parser.model.predicate_expression;

public enum ComparisonOperator {
    Equals("=="),
    NotEquals("!="),
    LessThan("<"),
    LessEquals("<="),
    GreaterThan(">"),
    GreaterEquals(">=");

    final String representation;

    ComparisonOperator(String representation) {
        this.representation = representation;
    }

    public String getRepresentation() {
        return representation;
    }
}
