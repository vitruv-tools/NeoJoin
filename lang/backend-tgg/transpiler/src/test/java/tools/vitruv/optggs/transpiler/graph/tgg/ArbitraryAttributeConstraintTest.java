package tools.vitruv.optggs.transpiler.graph.tgg;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.transpiler.graph.tgg.constraint.ArbitraryAttributeConstraint;

class ArbitraryAttributeConstraintTest {
    @Test
    public void testOrdering() {
        var constraint = new ArbitraryAttributeConstraint("foo");
        constraint.addParameter("a", ConstantExpression.Primitive(""));
        constraint.addParameter("c", ConstantExpression.Primitive(""));
        constraint.addParameter("return", ConstantExpression.Primitive(""));
        constraint.addParameter("self", ConstantExpression.Primitive(""));
        constraint.addParameter("b", ConstantExpression.Primitive(""));
        var parameters = constraint.getParameters().stream().toList();
        assertEquals("self", parameters.get(0).attribute());
        assertEquals("a", parameters.get(1).attribute());
        assertEquals("b", parameters.get(2).attribute());
        assertEquals("c", parameters.get(3).attribute());
        assertEquals("return", parameters.get(4).attribute());
    }
}
