package tools.vitruv.optggs.operators.selection;

import tools.vitruv.optggs.operators.Tuple;

import java.util.Collection;
import java.util.List;

public class ResolvedUnion {

    private final List<Tuple<Pattern, Pattern>> branches;

    public ResolvedUnion(List<Tuple<Pattern, Pattern>> branches) {
        this.branches = branches;
    }

    public Collection<Tuple<Pattern, Pattern>> branches() {
        return branches;
    }

    @Override
    public String toString() {
        return String.join(" UNION ", branches.stream().map((branch) -> {
            var source = branch.first();
            var target = branch.last();
            return source + " => " + target;
        }).toList());
    }
}
