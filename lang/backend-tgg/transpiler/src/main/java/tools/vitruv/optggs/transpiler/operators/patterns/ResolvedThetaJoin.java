package tools.vitruv.optggs.transpiler.operators.patterns;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.FunctionInvocation;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.constraint.ArbitraryAttributeConstraint;

public class ResolvedThetaJoin implements ResolvedPatternLink {
    private final FQN element;
    private final FunctionInvocation function;

    public ResolvedThetaJoin(FQN element, FunctionInvocation function) {
        this.element = element;
        this.function = function;
    }

    @Override
    public FQN element() {
        return element;
    }

    @Override
    public TGGNode extendSlice(TGGSlice slice, TGGNode lastNode) {
        final TGGNode node = slice.findByType(element).orElseGet(() -> slice.addNode(element));
        var constraint = new ArbitraryAttributeConstraint(function.name());
        for (var parameter : function.parameters()) {
            var argument = function.argument(parameter);
            if (argument instanceof FunctionInvocation.ConstantArgument c) {
                constraint.addParameter(parameter, new ConstantExpression(c.value()));
            } else if (argument instanceof FunctionInvocation.ConstrainedArgument arg) {
                final TGGNode refNode = slice.findByType(arg.node()).orElseThrow();
                constraint.addParameter(
                        parameter,
                        refNode.addVariableAttribute(arg.attribute(), LogicOperator.Equals));
            }
        }
        slice.addConstraint(constraint);
        return node;
    }

    @Override
    public String toString() {
        return " ⨝(" + function + ") " + element.fqn();
    }
}
