package tools.vitruv.optggs.transpiler.tgg;

import org.junit.jupiter.api.Test;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;

import static org.junit.jupiter.api.Assertions.*;

class AttributeConstraintTest {
    @Test
    public void testOrdering() {
        var constraint = new AttributeConstraint("foo");
        constraint.addParameter("a", ConstantExpression.Primitive(""));
        constraint.addParameter("c", ConstantExpression.Primitive(""));
        constraint.addParameter("return", ConstantExpression.Primitive(""));
        constraint.addParameter("self", ConstantExpression.Primitive(""));
        constraint.addParameter("b", ConstantExpression.Primitive(""));
        var parameters = constraint.parameters().stream().toList();
        assertEquals("self", parameters.get(0).attribute());
        assertEquals("a", parameters.get(1).attribute());
        assertEquals("b", parameters.get(2).attribute());
        assertEquals("c", parameters.get(3).attribute());
        assertEquals("return", parameters.get(4).attribute());
    }
}