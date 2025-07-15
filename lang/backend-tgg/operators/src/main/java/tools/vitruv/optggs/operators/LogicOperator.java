package tools.vitruv.optggs.operators;

public enum LogicOperator {
    Equals,
    NotEquals,
    LessThan,
    LessEquals,
    MoreThan,
    MoreEquals;

    public String print() {
        switch (this) {
            case Equals -> {
                return "==";
            }
            case NotEquals -> {
                return "!=";
            }
            case LessThan -> {
                return "<";
            }
            case LessEquals -> {
                return "<=";
            }
            case MoreThan -> {
                return ">";
            }
            case MoreEquals -> {
                return ">=";
            }
        }
        return "";
    }
}
