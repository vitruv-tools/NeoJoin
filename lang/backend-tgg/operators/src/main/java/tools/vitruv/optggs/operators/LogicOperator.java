package tools.vitruv.optggs.operators;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LogicOperator {
    Equals("=="),
    NotEquals("!="),
    LessThan("<"),
    LessEquals("<="),
    MoreThan(">"),
    MoreEquals(">=");

    final String representation;

    public String print() {
        return representation;
    }
}
