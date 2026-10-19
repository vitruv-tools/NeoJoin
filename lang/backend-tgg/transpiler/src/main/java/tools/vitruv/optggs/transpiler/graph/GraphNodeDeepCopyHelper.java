package tools.vitruv.optggs.transpiler.graph;

import java.util.HashMap;
import java.util.Map;

public abstract class GraphNodeDeepCopyHelper<N extends AbstractGraphNode<?, N>> {
    private final Map<N, N> oldToNewNodes = new HashMap<>();

    private final NameRepository copiedNameRepository;

    protected GraphNodeDeepCopyHelper(NameRepository copiedNameRepository) {
        this.copiedNameRepository = copiedNameRepository;
    }

    public NameRepository getCopiedNameRepository() {
        return copiedNameRepository;
    }

    public N getCopiedNode(N oldNode) {
        if (oldToNewNodes.containsKey(oldNode)) {
            return oldToNewNodes.get(oldNode);
        }

        final N copiedNode = oldNode.deepCopy(this);
        oldToNewNodes.put(oldNode, copiedNode);
        return copiedNode;
    }
}
