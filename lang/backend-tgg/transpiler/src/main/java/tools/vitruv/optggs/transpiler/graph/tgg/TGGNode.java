package tools.vitruv.optggs.transpiler.graph.tgg;

import lombok.Getter;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.AbstractGraphNode;
import tools.vitruv.optggs.transpiler.graph.Attribute;
import tools.vitruv.optggs.transpiler.graph.GraphNodeDeepCopyHelper;
import tools.vitruv.optggs.transpiler.graph.NameRepository;
import tools.vitruv.optggs.transpiler.graph.pattern.PatternLink;
import tools.vitruv.optggs.transpiler.graph.pattern.PatternNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public class TGGNode extends AbstractGraphNode<TGGLink, TGGNode> {
    private boolean green;

    TGGNode(
            String id,
            FQN type,
            boolean green,
            NameRepository nameRepository,
            ArrayList<TGGLink> links,
            LinkedHashSet<Attribute> attributes) {
        super(id, type, nameRepository, links, attributes);
        this.green = green;
    }

    private static TGGNode create(
            String id, FQN type, boolean green, NameRepository nameRepository) {
        return new TGGNode(
                id, type, green, nameRepository, new ArrayList<>(), new LinkedHashSet<>());
    }

    public static TGGNode Black(String id, FQN type, NameRepository nameRepository) {
        return create(id, type, false, nameRepository);
    }

    public static TGGNode Green(String id, FQN type, NameRepository nameRepository) {
        return create(id, type, true, nameRepository);
    }

    public TGGNode makeGreen() {
        this.green = true;
        links.forEach(TGGLink::makeGreen);
        return this;
    }

    public TGGNode makeBlack() {
        this.green = false;
        return this;
    }

    @Override
    public TGGNode deepCopy(GraphNodeDeepCopyHelper<TGGNode> copyHelper) {
        final ArrayList<TGGLink> copiedLinks =
                this.links.stream()
                        .map(link -> link.deepCopy(copyHelper))
                        .collect(Collectors.toCollection(ArrayList::new));
        final LinkedHashSet<Attribute> copiedAttributes =
                this.attributes.stream()
                        .map(Attribute::deepCopy)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        return new TGGNode(
                id,
                type,
                green,
                copyHelper.getCopiedNameRepository(),
                copiedLinks,
                copiedAttributes);
    }

    public PatternNode convertToPatternNode() {
        return convertToPatternNode(
                new TGGNodeToPatternNodeConversionHelper(nameRepository.deepCopy()));
    }

    PatternNode convertToPatternNode(TGGNodeToPatternNodeConversionHelper conversionHelper) {
        final ArrayList<PatternLink> copiedLinks =
                this.links.stream()
                        .map(link -> link.toPatternLink(conversionHelper))
                        .collect(Collectors.toCollection(ArrayList::new));
        final LinkedHashSet<Attribute> copiedAttributes =
                this.attributes.stream()
                        .map(Attribute::deepCopy)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        return new PatternNode(
                id,
                type,
                conversionHelper.getCopiedNameRepository(),
                copiedLinks,
                copiedAttributes);
    }

    @Override
    public String toString() {
        final String links = String.join(",", links().stream().map(Objects::toString).toList());
        final String attributes =
                String.join(",", attributes().stream().map(Objects::toString).toList());
        return "<"
                + (green ? "++" : "")
                + id
                + ": "
                + type.fqn()
                + ";"
                + links
                + ";"
                + attributes
                + ">";
    }
}
