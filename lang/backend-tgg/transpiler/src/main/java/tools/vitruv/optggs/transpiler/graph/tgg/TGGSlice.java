package tools.vitruv.optggs.transpiler.graph.tgg;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.selection.PatternLink;
import tools.vitruv.optggs.transpiler.graph.tgg.constraint.AttributeConstraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class TGGSlice {
    private final TripleRule.RuleExtender ruleExtender;
    private final ArrayList<TGGNode> nodes = new ArrayList<>();
    private final ArrayList<Correspondence> correspondences = new ArrayList<>();

    public TGGSlice(
            TripleRule.RuleExtender ruleExtender,
            Collection<TGGNode> initialNodes,
            Collection<Correspondence> initialCorrespondences) {
        this.ruleExtender = ruleExtender;
        this.nodes.addAll(initialNodes);
        this.correspondences.addAll(initialCorrespondences);
    }

    public Collection<TGGNode> nodes() {
        return nodes;
    }

    public Optional<TGGNode> findByType(FQN type) {
        for (var node : nodes) {
            if (node.getType().equals(type)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    public TGGNode addNode(FQN type) {
        var node = ruleExtender.addNode(type);
        nodes.add(node);
        return node;
    }

    public Correspondence addCorrespondence(TGGNode source, TGGNode target) {
        var correspondence = ruleExtender.addCorrespondence(source, target);
        correspondences.add(correspondence);
        return correspondence;
    }

    public AttributeConstraint addConstraint(AttributeConstraint constraint) {
        return ruleExtender.addConstraint(constraint);
    }

    public TGGSlice makeGreen() {
        nodes.forEach(TGGNode::makeGreen);
        correspondences.forEach(Correspondence::makeGreen);
        return this;
    }

    public TGGSlice makeBlack() {
        for (var node : nodes) {
            node.makeBlack();
            for (var link : node.links()) {
                link.makeBlack();
            }
        }
        for (var correspondence : correspondences) {
            correspondence.makeBlack();
        }
        return this;
    }

    public boolean hasAnyGreenElements() {
        final boolean anyNodesAreGreen = nodes.stream().anyMatch(TGGNode::isGreen);
        final boolean anyLinksAreGreen =
                nodes.stream()
                        .map(TGGNode::links)
                        .flatMap(Collection::stream)
                        .anyMatch(TGGLink::isGreen);
        final boolean anyCorrespondencesAreGreen =
                correspondences.stream().anyMatch(Correspondence::isGreen);
        return anyNodesAreGreen || anyLinksAreGreen || anyCorrespondencesAreGreen;
    }

    public <T> Collection<T> mapNodes(Function<TGGNode, T> function) {
        return nodes.stream().map(function).toList();
    }

    public <T> Collection<T> filterMapNodes(
            Predicate<TGGNode> predicate, Function<TGGNode, T> function) {
        return nodes.stream().filter(predicate).map(function).toList();
    }

    public Collection<TGGNode> findNodes(Predicate<TGGNode> predicate) {
        return nodes.stream().filter(predicate).toList();
    }

    public void extend(PatternLink pattern) {}

    @Override
    public String toString() {
        return nodes.toString();
    }
}
