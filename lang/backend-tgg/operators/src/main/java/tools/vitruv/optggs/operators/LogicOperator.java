package tools.vitruv.optggs.operators;

public enum LogicOperator {
    Equals,
    NotEquals,
    LessThan,
    LessEquals,
    MoreThan,
    MoreEquals;

    public String print() {
        return switch (this) {
            case Equals -> "==";
            case NotEquals -> "!=";
            case LessThan -> "<";
            case LessEquals -> "<=";
            case MoreThan -> ">";
            case MoreEquals -> ">=";
        };
    }
}
