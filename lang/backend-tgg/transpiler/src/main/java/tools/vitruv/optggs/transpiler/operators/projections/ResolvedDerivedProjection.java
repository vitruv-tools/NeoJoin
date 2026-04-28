package tools.vitruv.optggs.transpiler.operators.projections;

import tools.vitruv.optggs.operators.FunctionInvocation;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.expressions.ValueExpression;
import tools.vitruv.optggs.operators.projections.DerivedProjection;
import tools.vitruv.optggs.transpiler.operators.ResolvedProjection;
import tools.vitruv.optggs.transpiler.tgg.AttributeConstraint;
import tools.vitruv.optggs.transpiler.tgg.TripleRule;

public class ResolvedDerivedProjection implements ResolvedProjection {

    private static final String RETURN = "return";

    private final FunctionInvocation function;

    public ResolvedDerivedProjection(DerivedProjection projection) {
        this(projection.function());
    }

    public ResolvedDerivedProjection(FunctionInvocation function) {
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
            case FunctionInvocation.ConstrainedArgument(var node1, var attribute) ->
                (parameter.equals(RETURN) ? rule.allTargetsAsSlice() : rule.allSourcesAsSlice())
                    .findByType(node1)
                    .orElseThrow(() -> new RuntimeException("Derive projection: node " + node1.fqn() +
                        (parameter.equals(RETURN) ? " not found in target graph" : " not found in source graph")))
                    .addVariableAttribute(attribute, LogicOperator.Equals);
            case Object ignored when parameter.equals(RETURN) ->
                throw new RuntimeException("Derive projection: return attribute must be constrained argument in target graph");
            case FunctionInvocation.ConstantArgument(var value) -> new ConstantExpression(value);
        };
    }

    @Override
    public String toString() {
        return "Π(" + function + ")";
    }
}
