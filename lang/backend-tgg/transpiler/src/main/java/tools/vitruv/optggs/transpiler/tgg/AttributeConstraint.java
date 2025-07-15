package tools.vitruv.optggs.transpiler.tgg;

import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.expressions.ValueExpression;
import tools.vitruv.optggs.operators.expressions.VariableExpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AttributeConstraint {
    public final String name;
    public final List<Parameter> parameters;

    public AttributeConstraint(String name) {
        this(name, new ArrayList<>());
    }

    public AttributeConstraint(String name, List<Parameter> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    public String name() {
        return name;
    }

    public Collection<Parameter> parameters() {
        // Order parameters to have a deterministic order
        // This is because eMoflon::neo gives us these parameters based on the index in the invocation, which
        // can be different than the index in the definition. And of course, we don't get the name of the parameter.
        // Alphabetical ordering, but `self` is the first and `result` the last entry.
        // E.g.: (a, c, self, return, b) becomes (self, a, b, c, return)
        return parameters.stream().sorted((a, b) -> {
            if (a.attribute().equals("self")) {
                return -1;
            } else if (a.attribute().equals("return")) {
                return 1;
            } else if (b.attribute().equals("self")) {
                return 1;
            } else if (b.attribute().equals("return")) {
                return -1;
            } else {
                return a.attribute().compareTo(b.attribute());
            }
        }).toList();
    }

    public void addParameter(String name, ValueExpression value) {
        parameters.add(new Parameter(name, value));
    }

    public void propagateConstant(VariableExpression variable, ConstantExpression constant) {
        for (var param : parameters) {
            param.propagateConstant(variable, constant);
        }
    }

    @Override
    public String toString() {
        var params = parameters.stream().map((param) -> param.attribute() + ": " + param.value()).toList();
        return name + "(" + String.join(",", params) + ")";
    }
}
