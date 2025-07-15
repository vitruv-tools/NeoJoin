package tools.vitruv.optggs.transpiler.operators;

import tools.vitruv.optggs.transpiler.tgg.Slice;
import tools.vitruv.optggs.transpiler.tgg.TripleGrammar;
import tools.vitruv.optggs.transpiler.tgg.TripleRule;

import java.util.*;
import java.util.stream.Stream;

public class ResolvedView {
    private List<ResolvedQuery> queries;

    public ResolvedView(List<ResolvedQuery> queries) {
        this.queries = queries;
    }

    public List<ResolvedQuery> queries() {
        return Collections.unmodifiableList(queries);
    }

    /**
     * Transform this view to a triple grammar
     *
     * @param name name of the grammar
     * @return triple grammar
     */
    public TripleGrammar toGrammar(String name) {
        var rules = queries.stream().map(ResolvedQuery::toRules).flatMap(Collection::stream).toList();
        var sourceMetamodels = extractSourceMetamodels(rules);
        var targetMetamodels = extractTargetMetamodels(rules);
        return new TripleGrammar(name, sourceMetamodels, targetMetamodels, rules);
    }

    private Set<String> extractMetamodels(Stream<Slice> slices) {
        var metamodels = new HashSet<String>();
        slices.forEach(slice -> {
            metamodels.addAll(slice.mapNodes(node -> node.type().metamodelName()));
        });
        return metamodels;
    }

    private Set<String> extractSourceMetamodels(Collection<TripleRule> rules) {
        return extractMetamodels(rules.stream().map(TripleRule::allSourcesAsSlice));
    }

    private Set<String> extractTargetMetamodels(Collection<TripleRule> rules) {
        return extractMetamodels(rules.stream().map(TripleRule::allTargetsAsSlice));
    }
}
