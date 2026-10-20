package tools.vitruv.optggs.transpiler.emoflon;

import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.VariableExpression;
import tools.vitruv.optggs.transpiler.graph.Attribute;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleGrammar;
import tools.vitruv.optggs.transpiler.graph.tgg.constraint.NotEqualsConstraint;

import java.util.List;
import java.util.stream.Stream;

public class EmoflonTripleGraphGrammarPreprocessor {
    public static void preprocess(TripleGrammar grammar) {
        replaceNotEqualsAttributesInGreenNodesWithConstraints(grammar);
    }

    /**
     * Restriction by eMoflon: The operator "!=" is not allowed in green nodes
     *
     * <p>Therefore, we need to convert notEquals attributes in green nodes to attribute constraints
     */
    private static void replaceNotEqualsAttributesInGreenNodesWithConstraints(
            TripleGrammar grammar) {
        grammar.getRules()
                .forEach(
                        rule -> {
                            final List<TGGNode> allGreenSourceAndTargetNodes =
                                    Stream.concat(
                                                    rule.allSourcesAsSlice().nodes().stream(),
                                                    rule.allTargetsAsSlice().nodes().stream())
                                            .filter(TGGNode::isGreen)
                                            .toList();

                            for (TGGNode greenNode : allGreenSourceAndTargetNodes) {
                                for (Attribute greenNodeAttribute : greenNode.attributes()) {
                                    if (greenNodeAttribute
                                            .operator()
                                            .equals(LogicOperator.NotEquals)) {
                                        final VariableExpression variable =
                                                greenNode.addVariableAttribute(
                                                        greenNodeAttribute.name(),
                                                        LogicOperator.Equals);
                                        rule.addConstraintRule(
                                                new NotEqualsConstraint(
                                                        variable, greenNodeAttribute.value()));
                                        greenNode.removeAttribute(greenNodeAttribute);
                                    }
                                }
                            }
                        });
    }
}
