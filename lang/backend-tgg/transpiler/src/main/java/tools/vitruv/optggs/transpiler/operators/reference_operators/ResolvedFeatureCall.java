package tools.vitruv.optggs.transpiler.operators.reference_operators;

import java.util.Objects;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;
import tools.vitruv.optggs.transpiler.graph.tgg.GraphPathToNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRulesBuilder;

public class ResolvedFeatureCall implements ResolvedReferenceOperator {
    private final FQN element;

    public ResolvedFeatureCall(FQN element) {
        this.element = element;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResolvedFeatureCall other)) return false;
        return Objects.equals(element, other.element);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element);
    }

    @Override
    public String toString() {
        return "ResolvedFeatureCall(" + "element=" + element + ")";
    }

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
