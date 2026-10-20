package tools.vitruv.optggs.transpiler.graph;

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
    public String toString() {
        return "-[" + name + "]->" + target.getId();
    }
}
