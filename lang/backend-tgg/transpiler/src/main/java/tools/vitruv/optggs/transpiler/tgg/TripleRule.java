package tools.vitruv.optggs.transpiler.tgg;

import tools.vitruv.optggs.operators.FQN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class TripleRule {
    public interface RuleExtender {
        Node addNode(FQN type);

        Correspondence addCorrespondence(Node source, Node target);

        AttributeConstraint addConstraint(AttributeConstraint constraint);
    }

    private final NameRepository nameRepository = new NameRepository();
    private final List<Node> sourceNodes = new ArrayList<>();
    private final List<Node> targetNodes = new ArrayList<>();
    private final List<Correspondence> correspondences = new ArrayList<>();
    private final List<AttributeConstraint> constraints = new ArrayList<>();
    private boolean isLinkRule = false;

    private Node createNode(FQN type) {
        var name = nameRepository.getLower(type);
        return Node.Black(name, type, nameRepository);
    }

    private Node addSourceNode(FQN type) {
        var node = createNode(type);
        sourceNodes.add(node);
        return node;
    }

    private Node addTargetNode(FQN type) {
        var node = createNode(type);
        targetNodes.add(node);
        return node;
    }

    public Correspondence addCorrespondenceRule(Node source, Node target) {
        Correspondence correspondence = Correspondence.Black(source, target);
        correspondences.add(correspondence);
        return correspondence;
    }

    public AttributeConstraint addConstraintRule(AttributeConstraint constraint) {
        constraints.add(constraint);
        return constraint;
    }

    public Slice addSourceSlice(Collection<Node> initialNodes, Collection<Correspondence> initialCorrespondences) {
        return new Slice(new RuleExtender() {
            @Override
            public Node addNode(FQN type) {
                return addSourceNode(type);
            }

            @Override
            public Correspondence addCorrespondence(Node source, Node target) {
                return addCorrespondenceRule(source, target);
            }

            @Override
            public AttributeConstraint addConstraint(AttributeConstraint constraint) {
                return addConstraintRule(constraint);
            }
        }, initialNodes, initialCorrespondences);
    }

    public Slice addSourceSlice() {
        return addSourceSlice(List.of(), List.of());
    }

    public Slice addTargetSlice(Collection<Node> initialNodes, Collection<Correspondence> initalCorrespondences) {
        return new Slice(new RuleExtender() {
            @Override
            public Node addNode(FQN type) {
                return addTargetNode(type);
            }

            @Override
            public Correspondence addCorrespondence(Node source, Node target) {
                return addCorrespondenceRule(source, target);
            }

            @Override
            public AttributeConstraint addConstraint(AttributeConstraint constraint) {
                return addConstraintRule(constraint);
            }
        }, initialNodes, initalCorrespondences);
    }

    public Slice addTargetSlice() {
        return addTargetSlice(List.of(), List.of());
    }

    public Optional<Node> findSourceNodeByType(FQN type) {
        for (var node : sourceNodes) {
            if (node.type().equals(type)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    public Optional<Node> findTargetNodeByType(FQN type) {
        for (var node : targetNodes) {
            if (node.type().equals(type)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    public Slice allSourcesAsSlice() {
        return addSourceSlice(sourceNodes, correspondences);
    }

    public Slice allTargetsAsSlice() {
        return addSourceSlice(targetNodes, correspondences);
    }

    public Collection<Correspondence> correspondences() {
        return correspondences;
    }

    public Collection<AttributeConstraint> constraints() {
        return constraints;
    }

    public boolean isLinkRule() {
        return isLinkRule;
    }

    public void setLinkRule(boolean linkRule) {
        isLinkRule = linkRule;
    }

    @Override
    public String toString() {
        var c = (constraints.isEmpty() ? "" : " cs: " + constraints);
        return "src: " + sourceNodes + " tgt: " + targetNodes + " corr: " + correspondences + c;
    }
}
