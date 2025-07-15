package tools.vitruv.optggs.transpiler.tgg;

import tools.vitruv.optggs.operators.FQN;

import java.util.Objects;

public record CorrespondenceType(FQN source, FQN target) {
    public CorrespondenceType(Node source, Node target) {
        this(source.type(), target.type());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CorrespondenceType that)) return false;
        return Objects.equals(source, that.source) && Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }
}
