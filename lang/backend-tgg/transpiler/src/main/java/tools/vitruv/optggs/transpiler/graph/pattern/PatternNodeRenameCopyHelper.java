package tools.vitruv.optggs.transpiler.graph.pattern;

import lombok.Value;

import java.util.HashMap;
import java.util.Map;

@Value
public class PatternNodeRenameCopyHelper {
    Map<PatternNode, PatternNode> oldToNewNodes = new HashMap<>();

    public PatternNode getCopiedNode(PatternNode oldNode) {
        if (oldToNewNodes.containsKey(oldNode)) {
            return oldToNewNodes.get(oldNode);
        }

        final PatternNode copiedNode = oldNode.copyWithDifferentNames(this);
        oldToNewNodes.put(oldNode, copiedNode);
        return copiedNode;
    }
}
