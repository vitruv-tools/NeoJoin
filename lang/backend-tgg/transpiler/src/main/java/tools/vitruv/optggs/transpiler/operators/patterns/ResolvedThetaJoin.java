package tools.vitruv.optggs.transpiler.operators.patterns;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.FunctionInvocation;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.transpiler.tgg.AttributeConstraint;
import tools.vitruv.optggs.transpiler.tgg.Node;
import tools.vitruv.optggs.transpiler.tgg.Slice;

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
    public Node extendSlice(Slice slice, Node lastNode) {
        var node = slice.findByType(element).orElseGet(() -> slice.addNode(element));
        var constraint = new AttributeConstraint(function.name());
        for (var parameter : function.parameters()) {
            var argument = function.argument(parameter);
            switch (argument) {
                case FunctionInvocation.ConstantArgument(var value) ->
                    constraint.addParameter(parameter, new ConstantExpression(value));
                case FunctionInvocation.ConstrainedArgument(var node1, var attribute) -> {
                    Node refNode = slice.findByType(node1).orElseThrow();
                    constraint.addParameter(parameter, refNode.addVariableAttribute(attribute, LogicOperator.Equals));
                }
                case null, default -> {}
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
