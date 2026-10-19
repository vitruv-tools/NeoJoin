package tools.vitruv.optggs.transpiler.operators.reference_operators;

import lombok.Value;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.tgg.GraphPathToNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGLink;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRulesBuilder;

@Value
public class ResolvedCollectionMemberFeatureCall implements ResolvedReferenceOperator {
    String feature;
    FQN featureElement;

    @Override
    public void extendRules(TripleRulesBuilder builder) {
        final TripleRule rule = builder.getLatestRule().deepCopy().makeBlack();

        final GraphPathToNode pathToLastNode = builder.getPathToLastNode();
        final TGGNode lastSourceNode = rule.findNestedSourceNode(pathToLastNode);

        final TGGSlice sourceSlice = rule.addSourceSlice();
        TGGNode childNode = sourceSlice.addNode(featureElement);
        childNode.makeGreen();

        TGGLink parentLinkToChild = TGGLink.Green(feature, childNode);
        builder.addLinkToPathToLastNode(feature);
        lastSourceNode.addLink(parentLinkToChild);

        builder.addRule(rule);
    }
}
