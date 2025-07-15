package tools.vitruv.optggs.operators.selection;

import tools.vitruv.optggs.operators.FQN;

import java.util.Objects;

public class From implements PatternLink {
    private final FQN element;

    public From(FQN element) {
        this.element = element;
    }

    @Override
    public FQN element() {
        return element;
    }

    @Override
    public String toString() {
        return element.fqn();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof From from)) return false;
        return Objects.equals(element, from.element);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(element);
    }
}
