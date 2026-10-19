package tools.vitruv.optggs.transpiler.graph;

import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ValueExpression;

public record Attribute(String name, LogicOperator operator, ValueExpression value) {
    public Attribute deepCopy() {
        return new Attribute(name, operator, value);
    }

    @Override
    public String toString() {
        return "." + name + operator.print() + value;
    }
}
