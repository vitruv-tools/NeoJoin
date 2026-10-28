package tools.vitruv.optggs.operators.expressions;

import java.util.Objects;

/**
 * A variable expression
 *
 * <pre>
 * var variable = new VariableExpression("x");
 * </pre>
 */
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariableExpression other)) return false;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
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
