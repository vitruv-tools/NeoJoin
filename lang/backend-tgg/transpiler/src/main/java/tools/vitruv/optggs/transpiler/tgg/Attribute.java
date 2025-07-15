package tools.vitruv.optggs.transpiler.tgg;

import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.expressions.ValueExpression;
import tools.vitruv.optggs.operators.expressions.VariableExpression;

public class Attribute {
    private final String name;
    private final LogicOperator operator;
    private ValueExpression value;

    public Attribute(String name, LogicOperator operator, ValueExpression value) {
        this.name = name;
        this.operator = operator;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public LogicOperator operator() {
        return operator;
    }

    public ValueExpression value() {
        return value;
    }

    public void propagateConstant(VariableExpression variable, ConstantExpression constant) {
        if (value == variable) {
            value = constant;
        }
    }

    @Override
    public String toString() {
        return "." + name + operator.print() + value;
    }
}
