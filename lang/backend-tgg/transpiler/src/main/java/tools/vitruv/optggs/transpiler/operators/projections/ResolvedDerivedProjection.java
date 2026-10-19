package tools.vitruv.optggs.transpiler.operators.projections;

import tools.vitruv.optggs.operators.FunctionInvocation;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.projections.DerivedProjection;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;
import tools.vitruv.optggs.transpiler.graph.tgg.constraint.ArbitraryAttributeConstraint;
import tools.vitruv.optggs.transpiler.operators.ResolvedProjection;

public class ResolvedDerivedProjection implements ResolvedProjection {
    private final FunctionInvocation function;

    public ResolvedDerivedProjection(DerivedProjection projection) {
        this(projection.function());
    }

    public ResolvedDerivedProjection(FunctionInvocation function) {
        this.function = function;
    }

    @Override
    public void extendRule(TripleRule rule) {
        var constraint = new ArbitraryAttributeConstraint(function.name());
        for (var parameter : function.parameters()) {
            var argument = function.argument(parameter);
            if (parameter.equals("return")) {
                if (argument instanceof FunctionInvocation.ConstrainedArgument arg) {
                    final TGGNode node =
                            rule.allTargetsAsSlice()
                                    .findByType(arg.node())
                                    .orElseThrow(
                                            () ->
                                                    new RuntimeException(
                                                            "Derive projection: node "
                                                                    + arg.node().fqn()
                                                                    + " not found in target graph"));
                    constraint.addParameter(
                            parameter,
                            node.addVariableAttribute(arg.attribute(), LogicOperator.Equals));
                } else {
                    throw new RuntimeException(
                            "Derive projection: return attribute must be constrained argument in target graph");
                }
            } else if (argument instanceof FunctionInvocation.ConstantArgument c) {
                constraint.addParameter(parameter, new ConstantExpression(c.value()));
            } else if (argument instanceof FunctionInvocation.ConstrainedArgument arg) {
                final TGGNode node =
                        rule.allSourcesAsSlice()
                                .findByType(arg.node())
                                .orElseThrow(
                                        () ->
                                                new RuntimeException(
                                                        "Derive projection: node "
                                                                + arg.node().fqn()
                                                                + " not found in source graph"));
                constraint.addParameter(
                        parameter,
                        node.addVariableAttribute(arg.attribute(), LogicOperator.Equals));
            }
        }
        rule.addConstraintRule(constraint);
    }

    @Override
    public String toString() {
        return "Π(" + function + ")";
    }
}
