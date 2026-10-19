package tools.vitruv.optggs.transpiler.operators.reference_operators;

import lombok.Value;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;
import tools.vitruv.optggs.transpiler.graph.tgg.GraphPathToNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRulesBuilder;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGLink;

@Value
public class ResolvedCollectReferences implements ResolvedReferenceOperator {
    FQN targetRoot;
    FQN targetLeaf;
    String targetField;

    @Override
    public void extendRules(TripleRulesBuilder builder) {
        final TripleRule latestRule = builder.getLatestRule();

        final GraphPathToNode pathToLastNode = builder.getPathToLastNode();
        final TGGNode lastSourceNode = latestRule.findNestedSourceNode(pathToLastNode);
        lastSourceNode.makeBlack();

        final TGGNode targetSourceNode = latestRule.findTargetNodeByType(targetRoot).orElseThrow();

        final TGGSlice targetSlice = latestRule.addTargetSlice();
        TGGNode targetChildNode = targetSlice.addNode(targetLeaf);

        TGGLink targetParentLinkToChild = TGGLink.Green(targetField, targetChildNode);
        targetSourceNode.addLink(targetParentLinkToChild);

        latestRule.addCorrespondenceRule(lastSourceNode, targetChildNode);
    }
}
