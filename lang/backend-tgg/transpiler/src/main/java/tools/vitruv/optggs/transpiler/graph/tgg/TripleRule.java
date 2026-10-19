package tools.vitruv.optggs.transpiler.graph.tgg;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import org.jspecify.annotations.NonNull;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.NameRepository;
import tools.vitruv.optggs.transpiler.graph.pattern.GraphPattern;
import tools.vitruv.optggs.transpiler.graph.tgg.constraint.AttributeConstraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Builder(access = AccessLevel.PACKAGE)
public class TripleRule {
    public interface RuleExtender {
        TGGNode addNode(FQN type);

        Correspondence addCorrespondence(TGGNode source, TGGNode target);

        AttributeConstraint addConstraint(AttributeConstraint constraint);
    }

    @Getter @NonNull private final UUID id = UUID.randomUUID();
    @NonNull private final NameRepository nameRepository;
    @NonNull private final ArrayList<TGGNode> sourceNodes;
    @NonNull private final ArrayList<TGGNode> targetNodes;
    @NonNull private final ArrayList<Correspondence> correspondences;
    @NonNull private final ArrayList<AttributeConstraint> constraints;

    public TripleRule() {
        nameRepository = new NameRepository();
        sourceNodes = new ArrayList<>();
        targetNodes = new ArrayList<>();
        correspondences = new ArrayList<>();
        constraints = new ArrayList<>();
    }

    private TripleRule(
            NameRepository nameRepository,
            ArrayList<TGGNode> sourceNodes,
            ArrayList<TGGNode> targetNodes,
            ArrayList<Correspondence> correspondences,
            ArrayList<AttributeConstraint> constraints) {
        this.nameRepository = nameRepository;
        this.sourceNodes = sourceNodes;
        this.targetNodes = targetNodes;
        this.correspondences = correspondences;
        this.constraints = constraints;
    }

    private TGGNode createNode(FQN type) {
        var name = nameRepository.getLower(type);
        return TGGNode.Black(name, type, nameRepository);
    }

    private TGGNode addSourceNode(FQN type) {
        var node = createNode(type);
        sourceNodes.add(node);
        return node;
    }

    private TGGNode addTargetNode(FQN type) {
        var node = createNode(type);
        targetNodes.add(node);
        return node;
    }

    public Correspondence addCorrespondenceRule(TGGNode source, TGGNode target) {
        Correspondence correspondence = Correspondence.Black(source, target);
        correspondences.add(correspondence);
        return correspondence;
    }

    public AttributeConstraint addConstraintRule(AttributeConstraint constraint) {
        constraints.add(constraint);
        return constraint;
    }

    public TGGSlice addSourceSlice(
            Collection<TGGNode> initialNodes, Collection<Correspondence> initialCorrespondences) {
        return new TGGSlice(
                new RuleExtender() {
                    @Override
                    public TGGNode addNode(FQN type) {
                        return addSourceNode(type);
                    }

                    @Override
                    public Correspondence addCorrespondence(TGGNode source, TGGNode target) {
                        return addCorrespondenceRule(source, target);
                    }

                    @Override
                    public AttributeConstraint addConstraint(AttributeConstraint constraint) {
                        return addConstraintRule(constraint);
                    }
                },
                initialNodes,
                initialCorrespondences);
    }

    public TGGSlice addSourceSlice() {
        return addSourceSlice(List.of(), List.of());
    }

    public TGGSlice addTargetSlice(
            Collection<TGGNode> initialNodes, Collection<Correspondence> initalCorrespondences) {
        return new TGGSlice(
                new RuleExtender() {
                    @Override
                    public TGGNode addNode(FQN type) {
                        return addTargetNode(type);
                    }

                    @Override
                    public Correspondence addCorrespondence(TGGNode source, TGGNode target) {
                        return addCorrespondenceRule(source, target);
                    }

                    @Override
                    public AttributeConstraint addConstraint(AttributeConstraint constraint) {
                        return addConstraintRule(constraint);
                    }
                },
                initialNodes,
                initalCorrespondences);
    }

    public TGGSlice addTargetSlice() {
        return addTargetSlice(List.of(), List.of());
    }

    public Optional<TGGNode> findSourceNodeByType(FQN type) {
        return sourceNodes.stream().filter(node -> node.getType().equals(type)).findFirst();
    }

    public Optional<TGGNode> findTargetNodeByType(FQN type) {
        return targetNodes.stream().filter(node -> node.getType().equals(type)).findFirst();
    }

    public TGGSlice allSourcesAsSlice() {
        return addSourceSlice(sourceNodes, correspondences);
    }

    public TGGSlice allTargetsAsSlice() {
        return addSourceSlice(targetNodes, correspondences);
    }

    public Collection<Correspondence> correspondences() {
        return correspondences;
    }

    public Collection<AttributeConstraint> constraints() {
        return constraints;
    }

    public TripleRule makeBlack() {
        allSourcesAsSlice().makeBlack();
        allTargetsAsSlice().makeBlack();
        return this;
    }

    public TGGNode findNestedSourceNode(GraphPathToNode path) {
        TGGNode lastSourceNode = findSourceNodeByType(path.getRoot()).orElseThrow();
        for (String nextReference : path.getLinkPath()) {
            lastSourceNode = lastSourceNode.getFirstLinkTarget(nextReference);
        }
        return lastSourceNode;
    }

    public boolean hasAnyGreenElements() {
        return allSourcesAsSlice().hasAnyGreenElements()
                || allTargetsAsSlice().hasAnyGreenElements();
    }

    public TripleRule deepCopy() {
        final TGGNodeDeepCopyHelper copyHelper = new TGGNodeDeepCopyHelper(nameRepository);

        final ArrayList<TGGNode> newSourceNodes = new ArrayList<>();
        for (final TGGNode oldSourceNode : sourceNodes) {
            final TGGNode copiedSourceNode = copyHelper.getCopiedNode(oldSourceNode);
            newSourceNodes.add(copiedSourceNode);
        }

        final ArrayList<TGGNode> newTargetNodes = new ArrayList<>();
        for (final TGGNode oldTargetNode : targetNodes) {
            final TGGNode copiedTargetNode = copyHelper.getCopiedNode(oldTargetNode);
            newTargetNodes.add(copiedTargetNode);
        }

        final ArrayList<Correspondence> newCorrespondences = new ArrayList<>();
        for (final Correspondence correspondence : correspondences) {
            newCorrespondences.add(correspondence.deepCopy(copyHelper));
        }

        final ArrayList<AttributeConstraint> newConstraints = new ArrayList<>();
        for (final AttributeConstraint constraint : constraints) {
            newConstraints.add(constraint.deepCopy());
        }

        return new TripleRule(
                copyHelper.getCopiedNameRepository(),
                newSourceNodes,
                newTargetNodes,
                newCorrespondences,
                newConstraints);
    }

    public GraphPattern convertSourceNodesToGraphPattern() {
        final TGGNodeToPatternNodeConversionHelper conversionHelper =
                new TGGNodeToPatternNodeConversionHelper(nameRepository.deepCopy());
        return new GraphPattern(
                conversionHelper.getCopiedNameRepository(),
                sourceNodes.stream()
                        .map(conversionHelper::getConvertedNode)
                        .collect(Collectors.toCollection(ArrayList::new)));
    }

    @Override
    public String toString() {
        var c = (constraints.isEmpty() ? "" : " cs: " + constraints);
        return "src: " + sourceNodes + " tgt: " + targetNodes + " corr: " + correspondences + c;
    }
}
