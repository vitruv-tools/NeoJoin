package tools.vitruv.optggs.transpiler.graph.tgg.constraint;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import tools.vitruv.optggs.operators.expressions.ValueExpression;
import tools.vitruv.optggs.transpiler.graph.tgg.Parameter;

import java.util.List;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class NotEqualsConstraint implements AttributeConstraint {
    private static final String OTHER_PARAMETER_NAME = "other";

    String constraintName = "notEquals";
    List<Parameter> parameters;

    public NotEqualsConstraint(ValueExpression self, ValueExpression other) {
        this.parameters =
                List.of(
                        new Parameter(AttributeConstraint.SELF_PARAMETER_NAME, self),
                        new Parameter(OTHER_PARAMETER_NAME, other));
    }

    @Override
    public AttributeConstraint deepCopy() {
        return new NotEqualsConstraint(parameters.stream().map(Parameter::deepCopy).toList());
    }
}
