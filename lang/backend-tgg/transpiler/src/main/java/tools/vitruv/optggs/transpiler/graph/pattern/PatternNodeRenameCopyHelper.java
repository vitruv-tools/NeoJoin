package tools.vitruv.optggs.transpiler.graph.pattern;

import java.util.HashMap;
import java.util.Map;

public class PatternNodeRenameCopyHelper {
    private final Map<PatternNode, PatternNode> oldToNewNodes = new HashMap<>();

    public Map<PatternNode, PatternNode> getOldToNewNodes() {
        return oldToNewNodes;
    }

    public PatternNode getCopiedNode(PatternNode oldNode) {
        if (oldToNewNodes.containsKey(oldNode)) {
            return oldToNewNodes.get(oldNode);
        }

        final PatternNode copiedNode = oldNode.copyWithDifferentNames(this);
        oldToNewNodes.put(oldNode, copiedNode);
        return copiedNode;
    }
}
