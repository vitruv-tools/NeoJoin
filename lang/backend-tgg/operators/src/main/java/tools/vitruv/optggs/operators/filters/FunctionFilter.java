package tools.vitruv.optggs.operators.filters;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.FunctionInvocation;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.Filter;

public record FunctionFilter(FunctionInvocation function) implements Filter {

    public FunctionFilter(String function) {
        this(new FunctionInvocation(function));
    }

    public void setConstantArgument(String parameter, ConstantExpression value) {
        function.setConstantArgument(parameter, value.value());
    }

    public void setConstrainedArgument(String parameter, FQN element, String property) {
        function.setConstrainedArgument(parameter, element, property);
    }

    @Override
    public String toString() {
        return "φ(" + function + ")";
    }
}
