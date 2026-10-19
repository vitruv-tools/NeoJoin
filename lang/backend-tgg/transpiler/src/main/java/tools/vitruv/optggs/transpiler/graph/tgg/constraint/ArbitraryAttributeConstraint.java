package tools.vitruv.optggs.transpiler.graph.tgg.constraint;

import lombok.Value;

import tools.vitruv.optggs.operators.expressions.ValueExpression;
import tools.vitruv.optggs.transpiler.graph.tgg.Parameter;

import java.util.ArrayList;
import java.util.List;

@Value
public class ArbitraryAttributeConstraint implements AttributeConstraint {
    String constraintName;
    List<Parameter> parameters;

    public ArbitraryAttributeConstraint(String constraintName) {
        this(constraintName, new ArrayList<>());
    }

    public ArbitraryAttributeConstraint(String constraintName, List<Parameter> parameters) {
        this.constraintName = constraintName;
        this.parameters = parameters;
    }

    @Override
    public List<Parameter> getParameters() {
        // Order parameters to have a deterministic order
        // This is because eMoflon::neo gives us these parameters based on the index in the
        // invocation, which
        // can be different than the index in the definition. And of course, we don't get the name
        // of the parameter.
        // Alphabetical ordering, but `self` is the first and `result` the last entry.
        // E.g.: (a, c, self, return, b) becomes (self, a, b, c, return)
        return parameters.stream()
                .sorted(
                        (a, b) -> {
                            if (a.attribute().equals(AttributeConstraint.SELF_PARAMETER_NAME)) {
                                return -1;
                            } else if (a.attribute()
                                    .equals(AttributeConstraint.RETURN_PARAMETER_NAME)) {
                                return 1;
                            } else if (b.attribute()
                                    .equals(AttributeConstraint.SELF_PARAMETER_NAME)) {
                                return 1;
                            } else if (b.attribute()
                                    .equals(AttributeConstraint.RETURN_PARAMETER_NAME)) {
                                return -1;
                            } else {
                                return a.attribute().compareTo(b.attribute());
                            }
                        })
                .toList();
    }

    public void addParameter(String name, ValueExpression value) {
        parameters.add(new Parameter(name, value));
    }

    @Override
    public ArbitraryAttributeConstraint deepCopy() {
        return new ArbitraryAttributeConstraint(
                constraintName, parameters.stream().map(Parameter::deepCopy).toList());
    }

    @Override
    public String toString() {
        var params =
                parameters.stream()
                        .map((param) -> param.attribute() + ": " + param.value())
                        .toList();
        return constraintName + "(" + String.join(",", params) + ")";
    }
}
