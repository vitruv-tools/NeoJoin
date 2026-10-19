package tools.vitruv.optggs.operators;

public enum LogicOperator {
    Equals("=="),
    NotEquals("!="),
    LessThan("<"),
    LessEquals("<="),
    MoreThan(">"),
    MoreEquals(">=");

    final String representation;

    LogicOperator(String representation) {
        this.representation = representation;
    }

    public String getRepresentation() {
        return representation;
    }

    public String print() {
        return representation;
    }
}
