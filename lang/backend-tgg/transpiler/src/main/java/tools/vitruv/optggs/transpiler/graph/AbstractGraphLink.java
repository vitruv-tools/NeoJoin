package tools.vitruv.optggs.transpiler.graph;

import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
public abstract class AbstractGraphLink<N extends AbstractGraphNode<?, N>> {
    protected String name;
    protected N target;

    public abstract AbstractGraphLink<N> deepCopy(GraphNodeDeepCopyHelper<N> copyHelper);

    @Override
    public String toString() {
        return "-[" + name + "]->" + target.getId();
    }
}
