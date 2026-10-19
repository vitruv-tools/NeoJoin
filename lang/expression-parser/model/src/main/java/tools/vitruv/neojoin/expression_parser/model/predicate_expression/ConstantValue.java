package tools.vitruv.neojoin.expression_parser.model.predicate_expression;

import java.util.Objects;

public final class ConstantValue {
    private final String value;
    private final boolean isString;

    public ConstantValue(String value, boolean isString) {
        this.value = value;
        this.isString = isString;
    }

    public String getValue() {
        return value;
    }

    public boolean isString() {
        return isString;
    }

    public static ConstantValue fromString(String value) {
        return new ConstantValue(value, true);
    }

    public static ConstantValue fromBoolean(boolean isTrue) {
        return ConstantValue.of(Boolean.toString(isTrue));
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConstantValue that)) {
            return false;
        }
        return isString == that.isString && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, isString);
    }
}
