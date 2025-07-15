package tools.vitruv.optggs.transpiler.tgg;

import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.expressions.ValueExpression;
import tools.vitruv.optggs.operators.expressions.VariableExpression;

public final class Parameter {
    private final String attribute;
    private ValueExpression value;

    public Parameter(String attribute, ValueExpression value) {
        this.attribute = attribute;
        this.value = value;
    }

    public String attribute() {
        return attribute;
    }

    public ValueExpression value() {
        return value;
    }

    public void propagateConstant(VariableExpression variable, ConstantExpression constant) {
        if (value instanceof VariableExpression v) {
            if (v == variable) {
                value = constant;
            }
        }
    }

}
