package tools.vitruv.neojoin.expression_parser.model.predicate_expression;

import lombok.Value;

@Value
public class ConstantValue {
    String value;
    boolean isString;

    public static ConstantValue String(String value) {
        return new ConstantValue(value, true);
    }

    public static ConstantValue Boolean(boolean isTrue) {
        return ConstantValue.of(isTrue ? "true" : "false");
    }

    public static ConstantValue of(String value) {
        return new ConstantValue(value, false);
    }

    @Override
    public String toString() {
        if (isString) {
            return "\"" + value + "\"";
        }
        return value;
    }
}
