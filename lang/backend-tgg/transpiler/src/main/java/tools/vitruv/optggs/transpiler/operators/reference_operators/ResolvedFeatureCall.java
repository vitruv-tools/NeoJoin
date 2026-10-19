package tools.vitruv.optggs.transpiler.operators.reference_operators;

import lombok.Value;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;
import tools.vitruv.optggs.transpiler.graph.tgg.GraphPathToNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRulesBuilder;

@Value
public class ResolvedFeatureCall implements ResolvedReferenceOperator {
    FQN element;

    @Override
    public void extendRules(TripleRulesBuilder builder) {
        throw new IllegalStateException(
                "FeatureCall needs the target element to connect to, use custom method");
    }

    public TripleRule createFeatureCallRule(FQN targetTop, TripleRulesBuilder builder) {
        builder.setPathToLastNode(new GraphPathToNode(element));

        final TripleRule featureCallRule = builder.addRule();
        final TGGSlice sourceSlice = featureCallRule.addSourceSlice();
        final TGGNode sourceNode = sourceSlice.addNode(element);

        final TGGSlice targetSlice = featureCallRule.addTargetSlice();
        final TGGNode targetNode = targetSlice.addNode(targetTop);

        featureCallRule.addCorrespondenceRule(sourceNode, targetNode);

        return featureCallRule;
    }
}
