package tools.vitruv.optggs.operators.expressions;

import java.util.Objects;

/**
 * A constant expression
 *
 * <pre>
 * var constant = ConstantExpression.Primitive(5);
 * constant.value() == "5";
 * var constantString = ConstantExpression.String("Text");
 * constantString.value() == "\"Text\"";
 * </pre>
 */
public class ConstantExpression implements ValueExpression {
    private final String value;

    public ConstantExpression(String value) {
        this.value = value;
    }

    public static ConstantExpression String(String value) {
        return new ConstantExpression("\"" + value + "\"");
    }

    public static <T> ConstantExpression Primitive(T value) {
        return new ConstantExpression(value.toString());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConstantExpression other)) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public ValueExpression deepCopy() {
        return new ConstantExpression(value);
    }
}
