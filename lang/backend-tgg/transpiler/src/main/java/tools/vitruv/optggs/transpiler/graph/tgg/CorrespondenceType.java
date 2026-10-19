package tools.vitruv.optggs.transpiler.graph.tgg;

import tools.vitruv.optggs.operators.FQN;

import java.util.Objects;

public record CorrespondenceType(FQN source, FQN target) {
    public CorrespondenceType(TGGNode source, TGGNode target) {
        this(source.getType(), target.getType());
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
