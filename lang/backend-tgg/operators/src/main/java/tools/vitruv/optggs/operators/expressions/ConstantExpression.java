package tools.vitruv.optggs.operators.expressions;

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
    public String toString() {
        return value;
    }
}
