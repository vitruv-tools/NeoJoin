package tools.vitruv.optggs.transpiler.operators.reference_operators;

import java.util.Objects;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;
import tools.vitruv.optggs.transpiler.graph.tgg.GraphPathToNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRulesBuilder;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGLink;

public class ResolvedCollectReferences implements ResolvedReferenceOperator {
    private final FQN targetRoot;
    private final FQN targetLeaf;
    private final String targetField;

    public ResolvedCollectReferences(FQN targetRoot, FQN targetLeaf, String targetField) {
        this.targetRoot = targetRoot;
        this.targetLeaf = targetLeaf;
        this.targetField = targetField;
    }

    public FQN getTargetRoot() {
        return targetRoot;
    }

    public FQN getTargetLeaf() {
        return targetLeaf;
    }

    public String getTargetField() {
        return targetField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResolvedCollectReferences other)) return false;
        return Objects.equals(targetRoot, other.targetRoot) && Objects.equals(targetLeaf, other.targetLeaf) && Objects.equals(targetField, other.targetField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetRoot, targetLeaf, targetField);
    }

    @Override
    public String toString() {
        return "ResolvedCollectReferences(" + "targetRoot=" + targetRoot + ", " + "targetLeaf=" + targetLeaf + ", " + "targetField=" + targetField + ")";
    }

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
