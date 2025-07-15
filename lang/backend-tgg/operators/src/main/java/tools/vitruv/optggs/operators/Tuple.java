package tools.vitruv.optggs.operators;

import java.util.Objects;

public class Tuple<F, L> {
    private final F first;
    private final L last;

    public Tuple(F first, L last) {
        this.first = first;
        this.last = last;
    }

    public F first() {
        return first;
    }

    public L last() {
        return last;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tuple<?, ?> tuple)) return false;
        return Objects.equals(first, tuple.first) && Objects.equals(last, tuple.last);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, last);
    }
}
