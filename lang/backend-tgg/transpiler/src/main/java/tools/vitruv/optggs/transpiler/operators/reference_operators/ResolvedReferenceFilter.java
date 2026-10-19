package tools.vitruv.optggs.transpiler.operators.reference_operators;

import java.util.Objects;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.transpiler.graph.tgg.GraphPathToNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRulesBuilder;

public class ResolvedReferenceFilter implements ResolvedReferenceOperator {
    private final String feature;
    private final LogicOperator operator;
    private final ConstantExpression constantExpression;

    public ResolvedReferenceFilter(String feature, LogicOperator operator, ConstantExpression constantExpression) {
        this.feature = feature;
        this.operator = operator;
        this.constantExpression = constantExpression;
    }

    public String getFeature() {
        return feature;
    }

    public LogicOperator getOperator() {
        return operator;
    }

    public ConstantExpression getConstantExpression() {
        return constantExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResolvedReferenceFilter other)) return false;
        return Objects.equals(feature, other.feature) && Objects.equals(operator, other.operator) && Objects.equals(constantExpression, other.constantExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feature, operator, constantExpression);
    }

    @Override
    public String toString() {
        return "ResolvedReferenceFilter(" + "feature=" + feature + ", " + "operator=" + operator + ", " + "constantExpression=" + constantExpression + ")";
    }

    @Override
    public void extendRules(TripleRulesBuilder builder) {
        final TripleRule latestRule = builder.getLatestRule();

        final GraphPathToNode pathToLastNode = builder.getPathToLastNode();
        final TGGNode lastSourceNode = latestRule.findNestedSourceNode(pathToLastNode);

        lastSourceNode.addConstantAttribute(feature, operator, constantExpression);
    }
}
