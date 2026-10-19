package tools.vitruv.optggs.transpiler.graph;

import java.util.Objects;

public abstract class AbstractGraphLink<N extends AbstractGraphNode<?, N>> {
    protected final String name;
    protected final N target;

    protected AbstractGraphLink(String name, N target) {
        this.name = name;
        this.target = target;
    }

    public String getName() {
        return name;
    }

    public N getTarget() {
        return target;
    }

    public abstract AbstractGraphLink<N> deepCopy(GraphNodeDeepCopyHelper<N> copyHelper);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractGraphLink<?> other)) return false;
        return Objects.equals(name, other.name) && Objects.equals(target, other.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, target);
    }

    @Override
    public String toString() {
        return "-[" + name + "]->" + target.getId();
    }
}
