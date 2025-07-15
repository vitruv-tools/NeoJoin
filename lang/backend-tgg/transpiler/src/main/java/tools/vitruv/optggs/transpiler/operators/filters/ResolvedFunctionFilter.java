package tools.vitruv.optggs.transpiler.operators.filters;

import tools.vitruv.optggs.operators.FunctionInvocation;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.filters.FunctionFilter;
import tools.vitruv.optggs.transpiler.operators.ResolvedFilter;
import tools.vitruv.optggs.transpiler.tgg.AttributeConstraint;
import tools.vitruv.optggs.transpiler.tgg.Node;
import tools.vitruv.optggs.transpiler.tgg.TripleRule;

public class ResolvedFunctionFilter implements ResolvedFilter {
    private final FunctionInvocation function;

    public ResolvedFunctionFilter(FunctionFilter filter) {
        this(filter.function());
    }

    public ResolvedFunctionFilter(FunctionInvocation function) {
        this.function = function;
    }

    @Override
    public void extendRule(TripleRule rule) {
        var constraint = new AttributeConstraint(function.name());
        for (var parameter : function.parameters()) {
            var argument = function.argument(parameter);
            if (argument instanceof FunctionInvocation.ConstantArgument c) {
                constraint.addParameter(parameter, new ConstantExpression(c.value()));
            } else if (argument instanceof FunctionInvocation.ConstrainedArgument arg) {
                Node node = rule.allSourcesAsSlice().findByType(arg.node()).orElseThrow();
                constraint.addParameter(parameter, node.addVariableAttribute(arg.attribute(), LogicOperator.Equals));
            }
        }
        rule.addConstraintRule(constraint);
    }

    @Override
    public String toString() {
        return "Φ(" + function + ")";
    }

}
