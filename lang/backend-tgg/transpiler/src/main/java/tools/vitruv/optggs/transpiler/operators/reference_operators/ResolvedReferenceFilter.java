package tools.vitruv.optggs.transpiler.operators.reference_operators;

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

    @Override
    public void extendRules(TripleRulesBuilder builder) {
        final TripleRule latestRule = builder.getLatestRule();

        final GraphPathToNode pathToLastNode = builder.getPathToLastNode();
        final TGGNode lastSourceNode = latestRule.findNestedSourceNode(pathToLastNode);

        lastSourceNode.addConstantAttribute(feature, operator, constantExpression);
    }
}
