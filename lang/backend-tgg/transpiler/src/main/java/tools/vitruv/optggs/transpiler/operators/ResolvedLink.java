package tools.vitruv.optggs.transpiler.operators;

import tools.vitruv.optggs.transpiler.operators.patterns.ResolvedPattern;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;

import java.util.Collection;
import java.util.List;

public class ResolvedLink implements RuleGenerator {
    private final ResolvedPattern source;
    private final ResolvedPattern target;
    private final Collection<ResolvedFilter> filters;

    public ResolvedLink(ResolvedPattern source, ResolvedPattern target, Collection<ResolvedFilter> filters) {
        this.source = source;
        this.target = target;
        this.filters = filters;
    }

    public ResolvedPattern source() {
        return source;
    }

    public ResolvedPattern target() {
        return target;
    }

    @Override
    public void extendRule(TripleRule rule) {
        var sourceNode = rule.findSourceNodeByType(source.top()).orElseThrow();
        var targetNode = rule.findTargetNodeByType(target.top()).orElseThrow();
        var sourceSlice = rule.addSourceSlice(List.of(sourceNode), List.of());
        var targetSlice = rule.addTargetSlice(List.of(targetNode), List.of());
        extendSlice(sourceSlice, source);
        extendSlice(targetSlice, target);
        sourceSlice.addCorrespondence(sourceSlice.findByType(source.bottom()).orElseThrow(), targetSlice.findByType(target.bottom()).orElseThrow());
        for (var filter : filters) {
            filter.extendRule(rule);
        }
    }

    private void extendSlice(TGGSlice slice, ResolvedPattern pattern) {
        pattern.extendSlice(slice).makeGreen();
        slice.findByType(pattern.top()).orElseThrow().makeBlack();
        slice.findByType(pattern.bottom()).orElseThrow().makeBlack();
    }

    @Override
    public String toString() {
        var f = String.join(", ", filters.stream().map(Object::toString).toList());
        if (!f.isEmpty()) {
            f = " | " + f;
        }
        return "Λ(" + source + " => " + target + f + ")";
    }
}

