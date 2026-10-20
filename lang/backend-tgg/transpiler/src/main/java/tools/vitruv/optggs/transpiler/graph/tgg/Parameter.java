package tools.vitruv.optggs.transpiler.graph.tgg;

import tools.vitruv.optggs.operators.expressions.ValueExpression;

public record Parameter(String attribute, ValueExpression value) {
    public Parameter deepCopy() {
        return new Parameter(attribute, value.deepCopy());
    }
}
