package tools.vitruv.optggs.transpiler.tgg;

import tools.vitruv.optggs.operators.FQN;

import java.util.Objects;

public record CorrespondenceType(FQN source, FQN target) {
    public CorrespondenceType(Node source, Node target) {
        this(source.type(), target.type());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CorrespondenceType(FQN source1, FQN target1) &&
            Objects.equals(source, source1) && Objects.equals(target, target1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }
}
