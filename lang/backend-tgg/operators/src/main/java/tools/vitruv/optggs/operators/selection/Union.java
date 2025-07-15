package tools.vitruv.optggs.operators.selection;

import tools.vitruv.optggs.operators.Tuple;
import tools.vitruv.optggs.operators.Mapping;

import java.util.*;

public class Union {
    private final List<Pattern> branches = new ArrayList<>();

    public Union(Collection<Pattern> branches) {
        this.branches.addAll(branches);
    }

    public Union(Pattern source) {
        this(List.of(source));
    }

    public Collection<Pattern> sources() {
        return branches;
    }

    public Union add(Pattern source) {
        branches.add(source);
        return this;
    }

    public ResolvedUnion resolve(Set<Mapping> mappings, Resolver resolver) {
        return new ResolvedUnion(
                branches.stream().map((source) -> resolveBranch(source, mappings, resolver).orElseThrow()).toList()
        );
    }

    private Optional<Tuple<Pattern, Pattern>> resolveBranch(Pattern source, Set<Mapping> mappings, Resolver resolver) {
        for (var mapping : mappings) {
            if (resolver.matches(mapping, source)) {
                return Optional.of(new Tuple<>(source, resolver.map(mapping)));
            }
        }
        return Optional.empty();
    }

    public boolean anyEquals(Pattern pattern) {
        return branches.stream().anyMatch((branch) -> branch.equals(pattern));
    }

    public boolean anyStartWith(Pattern pattern) {
        return branches.stream().anyMatch((branch) -> branch.startsWith(pattern));
    }


    @Override
    public String toString() {
        return String.join(" UNION ", branches.stream().map(Object::toString).toList());
    }

    public interface Resolver {
        boolean matches(Mapping mapping, Pattern source);

        Pattern map(Mapping mapping);
    }
}
