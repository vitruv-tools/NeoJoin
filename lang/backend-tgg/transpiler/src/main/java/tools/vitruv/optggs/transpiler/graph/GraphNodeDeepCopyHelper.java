package tools.vitruv.optggs.transpiler.graph;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.HashMap;
import java.util.Map;

@Value
@NonFinal
public abstract class GraphNodeDeepCopyHelper<N extends AbstractGraphNode<?, N>> {
    @Getter(AccessLevel.NONE)
    Map<N, N> oldToNewNodes = new HashMap<>();

    NameRepository copiedNameRepository;

    public N getCopiedNode(N oldNode) {
        if (oldToNewNodes.containsKey(oldNode)) {
            return oldToNewNodes.get(oldNode);
        }

        final N copiedNode = oldNode.deepCopy(this);
        oldToNewNodes.put(oldNode, copiedNode);
        return copiedNode;
    }
}
