package tools.vitruv.optggs.transpiler.graph.tgg.constraint;

import tools.vitruv.optggs.operators.expressions.ValueExpression;
import tools.vitruv.optggs.transpiler.graph.tgg.Parameter;

import java.util.List;
import java.util.Objects;

public class NotEqualsConstraint implements AttributeConstraint {
    private static final String OTHER_PARAMETER_NAME = "other";

    private final String constraintName = "notEquals";
    private final List<Parameter> parameters;

    private NotEqualsConstraint(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public NotEqualsConstraint(ValueExpression self, ValueExpression other) {
        this.parameters =
                List.of(
                        new Parameter(AttributeConstraint.SELF_PARAMETER_NAME, self),
                        new Parameter(OTHER_PARAMETER_NAME, other));
    }

    @Override
    public String getConstraintName() {
        return constraintName;
    }

    @Override
    public List<Parameter> getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotEqualsConstraint other)) return false;
        return Objects.equals(constraintName, other.constraintName)
                && Objects.equals(parameters, other.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraintName, parameters);
    }

    @Override
    public String toString() {
        return "NotEqualsConstraint(constraintName="
                + constraintName
                + ", parameters="
                + parameters
                + ")";
    }

    @Override
    public AttributeConstraint deepCopy() {
        return new NotEqualsConstraint(parameters.stream().map(Parameter::deepCopy).toList());
    }
}
