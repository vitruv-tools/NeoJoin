package tools.vitruv.optggs.transpiler.graph.pattern;

import lombok.Value;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.NameRepository;
import tools.vitruv.optggs.transpiler.graph.tgg.GraphPathToNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Value
public class GraphPattern {
    UUID id = UUID.randomUUID();

    NameRepository nameRepository;
    ArrayList<PatternNode> nodes;

    public List<PatternNode> getNodes() {
        return nodes.stream().toList();
    }

    public GraphPattern addNode(PatternNode node) {
        nodes.add(node);
        nodes.addAll(node.collectLinkTargets());
        return this;
    }

    public Optional<PatternNode> findNodeByType(FQN type) {
        return nodes.stream().filter(node -> node.getType().equals(type)).findFirst();
    }

    public PatternNode findNestedNode(GraphPathToNode path) {
        PatternNode lastNode = findNodeByType(path.getRoot()).orElseThrow();
        for (String nextReference : path.getLinkPath()) {
            lastNode = lastNode.getFirstLinkTarget(nextReference);
        }
        return lastNode;
    }

    public GraphPattern duplicateAllNodesWithDifferentNames() {
        final PatternNodeRenameCopyHelper copyHelper = new PatternNodeRenameCopyHelper();
        final List<PatternNode> copiedNodesWithDifferentNames =
                nodes.stream()
                        .map(node -> node.copyWithDifferentNames(copyHelper))
                        .collect(Collectors.toCollection(ArrayList::new));
        nodes.addAll(copiedNodesWithDifferentNames);
        return this;
    }
}
