package tools.vitruv.optggs.transpiler.graph.tgg;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

import org.jspecify.annotations.Nullable;

import tools.vitruv.optggs.transpiler.graph.NameRepository;
import tools.vitruv.optggs.transpiler.graph.pattern.PatternNode;

import java.util.HashMap;
import java.util.Map;

@Value
public class TGGNodeToPatternNodeConversionHelper {
    @Getter(AccessLevel.NONE)
    Map<TGGNode, PatternNode> oldToNewNodes = new HashMap<>();

    NameRepository copiedNameRepository;

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
