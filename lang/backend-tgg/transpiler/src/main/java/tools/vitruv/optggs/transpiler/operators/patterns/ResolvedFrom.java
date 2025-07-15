package tools.vitruv.optggs.transpiler.operators.patterns;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.tgg.Node;
import tools.vitruv.optggs.transpiler.tgg.Slice;

import java.util.Objects;

public class ResolvedFrom implements ResolvedPatternLink {
    private final FQN element;

    public ResolvedFrom(FQN element) {
        this.element = element;
    }

    @Override
    public FQN element() {
        return element;
    }

    @Override
    public Node extendSlice(Slice slice, Node lastNode) {
        var node = slice.findByType(element);
        return node.orElseGet(() -> slice.addNode(element));
    }

    @Override
    public String toString() {
        return element.fqn();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ResolvedFrom from)) return false;
        return Objects.equals(element, from.element);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(element);
    }
}
