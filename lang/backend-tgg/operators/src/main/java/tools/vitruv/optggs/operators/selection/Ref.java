package tools.vitruv.optggs.operators.selection;

import tools.vitruv.optggs.operators.FQN;

import java.util.Objects;

public class Ref implements PatternLink {
    private final FQN element;
    private final String reference;

    public Ref(FQN element, String reference) {
        this.element = element;
        this.reference = reference;
    }

    @Override
    public FQN element() {
        return element;
    }

    public String reference() {
        return reference;
    }

    @Override
    public String toString() {
        return "-[" + reference + "]->" + element.fqn();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ref ref)) return false;
        return Objects.equals(element, ref.element) && Objects.equals(reference, ref.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, reference);
    }
}
