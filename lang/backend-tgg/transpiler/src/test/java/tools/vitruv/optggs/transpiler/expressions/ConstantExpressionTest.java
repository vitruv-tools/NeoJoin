package tools.vitruv.optggs.transpiler.expressions;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstantExpressionTest {
    @Test
    void primitive() {
        assertEquals("5", ConstantExpression.Primitive(5).value());
    }

    @Test
    void primitiveString() {
        assertEquals("5", ConstantExpression.Primitive("5").value());
        assertEquals("Foo", ConstantExpression.Primitive("Foo").value());
    }

    @Test
    void string() {
        assertEquals("\"5\"", ConstantExpression.String("5").value());
        assertEquals("\"Foo\"", ConstantExpression.String("Foo").value());
    }

}
