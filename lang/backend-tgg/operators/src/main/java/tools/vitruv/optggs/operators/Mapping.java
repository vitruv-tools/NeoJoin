package tools.vitruv.optggs.operators;

import java.util.Objects;

/**
 * Mapping between a source and target element
 * <br/>
 * Example:
 * This query
 * <pre>
 * σ(A => A')
 * </pre>
 * has the mapping
 * <pre>
 * (A,A')
 * </pre>
 * However, more complex queries can have multiple mappings:
 * <pre>
 * σ(A ⨝(id) B => A')
 * </pre>
 * has the mappings
 * <pre>
 * { (A,A'), (B,A') }
 * </pre>
 */
public record Mapping(FQN source, FQN target) {

    @Override
    public String toString() {
        return source.fqn() + " => " + target.fqn();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Mapping mapping)) return false;
        return Objects.equals(source, mapping.source) && Objects.equals(target, mapping.target);
    }

}
