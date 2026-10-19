package tools.vitruv.optggs.transpiler.operators.filters;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.filters.ConstantFilter;
import tools.vitruv.optggs.transpiler.operators.ResolvedFilter;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;

public class ResolvedConstantFilter implements ResolvedFilter {
    private final FQN element;
    private final String property;
    private final LogicOperator operator;
    private final ConstantExpression value;

    public ResolvedConstantFilter(ConstantFilter filter) {
        this(filter.element(), filter.property(), filter.operator(), filter.value());
    }

    public ResolvedConstantFilter(FQN element, String property, LogicOperator operator, ConstantExpression value) {
        this.element = element;
        this.property = property;
        this.operator = operator;
        this.value = value;
    }

    @Override
    public void extendRule(TripleRule rule) {
        var node = rule.findSourceNodeByType(element).orElseThrow(() -> new RuntimeException("Filter: element " + element.fqn() + " not found in source graph"));
        node.addConstantAttribute(property, operator, value);
    }

    @Override
    public String toString() {
        return "Φ(" + element.fqn() + "::" + property + operator.print() + value.value() + ")";
    }
}
