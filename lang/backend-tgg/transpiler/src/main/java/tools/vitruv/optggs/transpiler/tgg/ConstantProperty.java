package tools.vitruv.optggs.transpiler.tgg;

import tools.vitruv.optggs.operators.LogicOperator;

public record ConstantProperty(String name, LogicOperator operator, String value) implements Property {

    @Override
    public String toExpression(Node node) {

        if (node.isGreen()) {
            return "." + name + " := " + value;
        } else {
            return "." + name + " : " + value;
        }
    }
}
