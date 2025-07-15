package tools.vitruv.optggs.operators.filters;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.Filter;

public record ConstantFilter(FQN element,
                             String property,
                             LogicOperator operator,
                             ConstantExpression value) implements Filter {

    @Override
    public String toString() {
        return "φ(" + element.fqn() + "::" + property + operator.print() + value.value() + ")";
    }
}
