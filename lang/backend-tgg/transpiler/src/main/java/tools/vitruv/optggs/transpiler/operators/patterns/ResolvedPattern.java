package tools.vitruv.optggs.transpiler.operators.patterns;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.Tuple;
import tools.vitruv.optggs.operators.selection.Pattern;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ResolvedPattern {
    private final List<ResolvedPatternLink> links;

    public ResolvedPattern(List<ResolvedPatternLink> links) {
        this.links = links;
    }

    public TGGSlice extendSlice(TGGSlice slice) {
        TGGNode node = null;
        for (var link : links) {
            node = link.extendSlice(slice, node);
        }
        return slice;
    }

    public FQN top() {
        return links.getFirst().element();
    }

    public FQN bottom() {
        return links.getLast().element();
    }

    public Pattern topPattern() {
        return Pattern.from(top());
    }

    public Pattern bottomPattern() {
        return Pattern.from(bottom());
    }

    public int length() {
        return links.size();
    }

    public Collection<FQN> elements() {
        return links.stream().map(ResolvedPatternLink::element).toList();
    }

    public Tuple<ResolvedPattern, ResolvedPatternLink> popBottom() {
        var last = links.get(links.size() - 1);
        var remainder = links.subList(0, links.size() - 1);
        return new Tuple<>(new ResolvedPattern(remainder), last);
    }

    public Tuple<ResolvedPattern, ResolvedPatternLink> popTop() {
        var first = links.get(0);
        var remainder = links.subList(1, links.size());
        return new Tuple<>(new ResolvedPattern(remainder), first);
    }

    public boolean startsWith(ResolvedPattern other) {
        if (this.links.size() < other.links.size()) return false;
        for (var i = 0; i < other.links.size(); ++i) {
            if (!this.links.get(i).equals(other.links.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return String.join("", links.stream().map(Object::toString).toList());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ResolvedPattern pattern)) return false;
        return Objects.equals(links, pattern.links);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(links);
    }
}
