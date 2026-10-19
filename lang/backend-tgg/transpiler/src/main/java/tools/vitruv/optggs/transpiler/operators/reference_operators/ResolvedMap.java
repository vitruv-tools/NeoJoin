package tools.vitruv.optggs.transpiler.operators.reference_operators;

import java.util.Objects;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;
import tools.vitruv.optggs.transpiler.graph.tgg.GraphPathToNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRulesBuilder;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGLink;

public class ResolvedMap implements ResolvedReferenceOperator {
    private final String feature;
    private final FQN featureElement;

    public ResolvedMap(String feature, FQN featureElement) {
        this.feature = feature;
        this.featureElement = featureElement;
    }

    public String getFeature() {
        return feature;
    }

    public FQN getFeatureElement() {
        return featureElement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResolvedMap other)) return false;
        return Objects.equals(feature, other.feature) && Objects.equals(featureElement, other.featureElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feature, featureElement);
    }

    @Override
    public String toString() {
        return "ResolvedMap(" + "feature=" + feature + ", " + "featureElement=" + featureElement + ")";
    }

    @Override
    public void extendRules(TripleRulesBuilder builder) {
        final TripleRule latestRule = builder.getLatestRule();

        final GraphPathToNode pathToLastNode = builder.getPathToLastNode();
        final TGGNode lastSourceNode = latestRule.findNestedSourceNode(pathToLastNode);

        final TGGSlice sourceSlice = latestRule.addSourceSlice();
        TGGNode childNode = sourceSlice.addNode(featureElement);
        childNode.makeGreen();

        TGGLink parentLinkToChild = TGGLink.Green(feature, childNode);
        builder.addLinkToPathToLastNode(feature);
        lastSourceNode.addLink(parentLinkToChild);
    }
}
