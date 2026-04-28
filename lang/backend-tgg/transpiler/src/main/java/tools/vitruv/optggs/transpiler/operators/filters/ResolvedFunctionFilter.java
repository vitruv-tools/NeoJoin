package tools.vitruv.optggs.transpiler.operators.filters;

import tools.vitruv.optggs.operators.FunctionInvocation;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.expressions.ValueExpression;
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
            var value = determineValueForFunctionParameter(rule, parameter);
            constraint.addParameter(parameter, value);
        }
        rule.addConstraintRule(constraint);
    }

    private ValueExpression determineValueForFunctionParameter(TripleRule rule, String parameter) {
        return switch (function.argument(parameter)) {
            case FunctionInvocation.ConstantArgument(var value) -> new ConstantExpression(value);
            case FunctionInvocation.ConstrainedArgument(var node1, var attribute) -> rule.allSourcesAsSlice()
                .findByType(node1).orElseThrow()
                .addVariableAttribute(attribute, LogicOperator.Equals);
        };
    }

    public String toString() {
        return "Φ(" + function + ")";
    }
}
