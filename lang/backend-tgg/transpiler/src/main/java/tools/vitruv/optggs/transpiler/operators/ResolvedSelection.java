package tools.vitruv.optggs.transpiler.operators;

import tools.vitruv.optggs.operators.Mapping;
import tools.vitruv.optggs.transpiler.operators.patterns.ResolvedPattern;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;

import java.util.HashSet;
import java.util.Set;

public class ResolvedSelection implements RuleGenerator {
    private final ResolvedPattern source;
    private final ResolvedPattern target;

    public ResolvedSelection(ResolvedPattern source, ResolvedPattern target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Get all mappings that this selection creates Example:
     *
     * <pre>
     * Σ(A ⨝(id) B-[c]->C => A'-[b]->B')
     * </pre>
     *
     * will yield these mappings:
     *
     * <pre>
     * { (A,A'), (A,B'), (B,A'), (B,B'), (C,A'), (C,B') }
     * </pre>
     *
     * @return set of mappings
     */
    public Set<Mapping> mappings() {
        var mappings = new HashSet<Mapping>();
        for (var sourceElement : source.elements()) {
            for (var targetElement : target.elements()) {
                mappings.add(new Mapping(sourceElement, targetElement));
            }
        }
        return mappings;
    }

    @Override
    public void extendRule(TripleRule rule) {
        TGGSlice sourceSlice = rule.addSourceSlice();
        TGGSlice targetSlice = rule.addTargetSlice();

        source.extendSlice(sourceSlice);
        target.extendSlice(targetSlice);
        for (var mapping : mappings()) {
            var sourceNode = sourceSlice.findByType(mapping.source()).orElseThrow();
            var targetNode = targetSlice.findByType(mapping.target()).orElseThrow();
            sourceSlice.addCorrespondence(sourceNode, targetNode);
        }
    }

    @Override
    public String toString() {
        return "Σ(" + source + " => " + target + ")";
    }
}
