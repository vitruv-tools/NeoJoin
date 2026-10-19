package tools.vitruv.optggs.operators.expressions;

import lombok.EqualsAndHashCode;

/**
 * A variable expression
 *
 * <pre>
 * var variable = new VariableExpression("x");
 * </pre>
 */
@EqualsAndHashCode
public class VariableExpression implements ValueExpression {
    private final String name;

    /**
     * Creates a variable with the given name
     *
     * @param name Name of the variable
     */
    public VariableExpression(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "<" + name + ">";
    }

    @Override
    public ValueExpression deepCopy() {
        return new VariableExpression(name);
    }
}
