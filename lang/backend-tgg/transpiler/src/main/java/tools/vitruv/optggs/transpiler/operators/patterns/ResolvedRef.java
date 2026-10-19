package tools.vitruv.optggs.transpiler.operators.patterns;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGLink;

import java.util.Objects;

public class ResolvedRef implements ResolvedPatternLink {
    private final FQN element;
    private final String reference;

    public ResolvedRef(FQN element, String reference) {
        this.element = element;
        this.reference = reference;
    }

    @Override
    public TGGNode extendSlice(TGGSlice slice, TGGNode lastNode) {
        final TGGNode node = slice.findByType(element).orElseGet(() -> slice.addNode(element));
        lastNode.addLink(TGGLink.Black(reference, node));
        return node;
    }

    @Override
    public FQN element() {
        return element;
    }

    @Override
    public String toString() {
        return "-[" + reference + "]->" + element.fqn();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ResolvedRef ref)) return false;
        return Objects.equals(element, ref.element) && Objects.equals(reference, ref.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, reference);
    }
}
