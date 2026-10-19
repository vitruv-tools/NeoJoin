package tools.vitruv.optggs.transpiler.operators;

import tools.vitruv.optggs.operators.Mapping;
import tools.vitruv.optggs.operators.traits.Mappable;
import tools.vitruv.optggs.transpiler.operators.patterns.ResolvedPattern;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;

import java.util.Collection;
import java.util.List;

public class ResolvedContainment implements RuleGenerator, Mappable {
    private final ResolvedPattern source;
    private final ResolvedPattern target;
    private final Collection<ResolvedFilter> filters;

    public ResolvedContainment(ResolvedPattern source, ResolvedPattern target, Collection<ResolvedFilter> filters) {
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
    public Mapping mapping() {
        return new Mapping(source.bottom(), target.bottom());
    }

    @Override
    public void extendRule(TripleRule rule) {
        var sourceNode = rule.findSourceNodeByType(source.bottom()).orElseThrow();
        var targetNode = rule.findTargetNodeByType(target.bottom()).orElseThrow();
        var sourceSlice = rule.addSourceSlice(List.of(sourceNode), List.of());
        var targetSlice = rule.addTargetSlice(List.of(targetNode), List.of());
        extendSlice(sourceSlice, source);
        extendSlice(targetSlice, target);
        sourceSlice.addCorrespondence(sourceSlice.findByType(source.top()).orElseThrow(), targetSlice.findByType(target.top()).orElseThrow());
        for (var filter : filters) {
            filter.extendRule(rule);
        }
    }

    private void extendSlice(TGGSlice slice, ResolvedPattern pattern) {
        pattern.extendSlice(slice).makeGreen();
        slice.findByType(pattern.top()).orElseThrow().makeBlack();
    }

    @Override
    public String toString() {
        var f = String.join(", ", filters.stream().map(Object::toString).toList());
        if (!f.isEmpty()) {
            f = " | " + f;
        }
        return "Κ(" + source + " => " + target + f + ")";
    }
}
