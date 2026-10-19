package tools.vitruv.optggs.transpiler.graph.tgg;

import org.jspecify.annotations.Nullable;

import tools.vitruv.optggs.transpiler.graph.NameRepository;
import tools.vitruv.optggs.transpiler.graph.pattern.PatternNode;

import java.util.HashMap;
import java.util.Map;

public class TGGNodeToPatternNodeConversionHelper {
    private final Map<TGGNode, PatternNode> oldToNewNodes = new HashMap<>();

    private final NameRepository copiedNameRepository;

    public TGGNodeToPatternNodeConversionHelper(NameRepository copiedNameRepository) {
        this.copiedNameRepository = copiedNameRepository;
    }

    public NameRepository getCopiedNameRepository() {
        return copiedNameRepository;
    }

    /**
     * Converts a TGGNode into a PatternNode. If the Node was already converted, this is returned.
     * Otherwise, the TGGNode is copied and converted and stored.
     */
    @Nullable
    public PatternNode getConvertedNode(TGGNode oldNode) {
        if (oldToNewNodes.containsKey(oldNode)) {
            return oldToNewNodes.get(oldNode);
        }

        final PatternNode copiedNode = oldNode.convertToPatternNode(this);
        oldToNewNodes.put(oldNode, copiedNode);
        return copiedNode;
    }
}
