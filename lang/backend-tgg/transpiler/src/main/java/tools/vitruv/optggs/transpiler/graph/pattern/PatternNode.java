package tools.vitruv.optggs.transpiler.graph.pattern;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.AbstractGraphNode;
import tools.vitruv.optggs.transpiler.graph.Attribute;
import tools.vitruv.optggs.transpiler.graph.GraphNodeDeepCopyHelper;
import tools.vitruv.optggs.transpiler.graph.NameRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class PatternNode extends AbstractGraphNode<PatternLink, PatternNode> {
    public PatternNode(
            String id,
            FQN type,
            NameRepository nameRepository,
            ArrayList<PatternLink> links,
            LinkedHashSet<Attribute> attributes) {
        super(id, type, nameRepository, links, attributes);
    }

    public static PatternNode create(String id, FQN type, NameRepository nameRepository) {
        return new PatternNode(id, type, nameRepository, new ArrayList<>(), new LinkedHashSet<>());
    }

    public PatternNode copyWithDifferentNames() {
        return copyWithDifferentNames(new PatternNodeRenameCopyHelper());
    }

    PatternNode copyWithDifferentNames(PatternNodeRenameCopyHelper copyAndRenameHelper) {
        if (copyAndRenameHelper.getOldToNewNodes().containsKey(this)) {
            return copyAndRenameHelper.getOldToNewNodes().get(this);
        }

        final ArrayList<PatternLink> copiedLinks =
                this.links.stream()
                        .map(link -> link.copyWithDifferentNames(copyAndRenameHelper))
                        .collect(Collectors.toCollection(ArrayList::new));
        final LinkedHashSet<Attribute> copiedAttributes =
                this.attributes.stream()
                        .map(Attribute::deepCopy)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        return new PatternNode(
                nameRepository.get(id), type, nameRepository, copiedLinks, copiedAttributes);
    }

    List<PatternNode> collectLinkTargets() {
        final ArrayList<PatternNode> results = new ArrayList<>();

        final ArrayList<PatternNode> directTargets =
                links.stream()
                        .map(PatternLink::getTarget)
                        .collect(Collectors.toCollection(ArrayList::new));
        results.addAll(directTargets);

        final ArrayList<PatternNode> recursiveTargets =
                directTargets.stream()
                        .map(PatternNode::collectLinkTargets)
                        .flatMap(List::stream)
                        .collect(Collectors.toCollection(ArrayList::new));
        results.addAll(recursiveTargets);

        return results;
    }

    public GraphPattern toGraphPattern() {
        final ArrayList<PatternNode> allNodes = new ArrayList<>();
        allNodes.add(this);
        allNodes.addAll(collectLinkTargets());
        return new GraphPattern(nameRepository, allNodes);
    }

    @Override
    public PatternNode deepCopy(GraphNodeDeepCopyHelper<PatternNode> copyHelper) {
        final ArrayList<PatternLink> copiedLinks =
                this.links.stream()
                        .map(link -> link.deepCopy(copyHelper))
                        .collect(Collectors.toCollection(ArrayList::new));
        final LinkedHashSet<Attribute> copiedAttributes =
                this.attributes.stream()
                        .map(Attribute::deepCopy)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        return new PatternNode(
                id, type, copyHelper.getCopiedNameRepository(), copiedLinks, copiedAttributes);
    }
}
