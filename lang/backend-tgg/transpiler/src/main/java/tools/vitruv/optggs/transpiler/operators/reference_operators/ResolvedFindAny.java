package tools.vitruv.optggs.transpiler.operators.reference_operators;

import tools.vitruv.optggs.transpiler.graph.pattern.GraphPattern;
import tools.vitruv.optggs.transpiler.graph.pattern.PatternLink;
import tools.vitruv.optggs.transpiler.graph.pattern.PatternNode;
import tools.vitruv.optggs.transpiler.graph.tgg.GraphPathToNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRulesBuilder;

import java.util.ArrayList;
import java.util.List;

public class ResolvedFindAny implements ResolvedReferenceOperator {
    @Override
    public void extendRules(TripleRulesBuilder builder) {
        final TripleRule latestRule = builder.getLatestRule();

        final GraphPathToNode pathToLastNode = builder.getPathToLastNode();
        final TGGNode nodeBeforeLastNode =
                latestRule.findNestedSourceNode(pathToLastNode.pathToSecondLastNode());
        final String lastLink = pathToLastNode.getLastLink();

        // Pattern with the last link duplicated (prevents two outgoing edges from the same node)
        final GraphPattern patternWithSecondLinkFromSameRoot =
                nodeBeforeLastNode.convertToPatternNode().toGraphPattern();
        latestRule.convertSourceNodesToGraphPattern();
        final PatternNode patternWithSecondLinkFromSameRootLastNode =
                patternWithSecondLinkFromSameRoot.findNestedNode(
                        new GraphPathToNode(
                                nodeBeforeLastNode.getType(), new ArrayList<>(List.of(lastLink))));

        final PatternNode patternWithSecondLinkFromSameRootCopiedLastNode =
                patternWithSecondLinkFromSameRootLastNode.copyWithDifferentNames();
        patternWithSecondLinkFromSameRoot.addNode(patternWithSecondLinkFromSameRootCopiedLastNode);
        final PatternNode patternWithSecondLinkFromSameRootRootNode =
                patternWithSecondLinkFromSameRoot
                        .findNodeByType(nodeBeforeLastNode.getType())
                        .orElseThrow();
        patternWithSecondLinkFromSameRootRootNode.addLink(
                new PatternLink(lastLink, patternWithSecondLinkFromSameRootCopiedLastNode));

        // Pattern with two matching graphs (prevents two outgoing edges from different nodes);
        final GraphPattern patternWithTwoSameGraphs =
                nodeBeforeLastNode
                        .convertToPatternNode()
                        .toGraphPattern()
                        .duplicateAllNodesWithDifferentNames();

        // Add constraints to the grammar
        builder.addConstraint(patternWithSecondLinkFromSameRoot);
        builder.addConstraint(patternWithTwoSameGraphs);
    }
}
