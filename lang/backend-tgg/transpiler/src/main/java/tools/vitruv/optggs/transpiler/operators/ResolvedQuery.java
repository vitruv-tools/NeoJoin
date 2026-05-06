package tools.vitruv.optggs.transpiler.operators;

import tools.vitruv.optggs.transpiler.tgg.TripleRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResolvedQuery {
    private final ResolvedSelection selection;
    private final List<ResolvedProjection> projections;
    private final List<ResolvedFilter> filters;
    private final Optional<ResolvedContainment> container;
    private final List<ResolvedLink> links;

    public ResolvedQuery(ResolvedSelection selection, List<ResolvedProjection> projections, List<ResolvedFilter> filters, Optional<ResolvedContainment> container, List<ResolvedLink> links) {
        this.selection = selection;
        this.projections = projections;
        this.filters = filters;
        this.container = container;
        this.links = links;
    }

    public Collection<TripleRule> toRules() {
        var rules = new ArrayList<TripleRule>();
        rules.add(createPrimaryRule());
        rules.addAll(links.stream().map(this::createLinkRule).toList());
        return rules;
    }

    public TripleRule createPrimaryRule() {
        var rule = new TripleRule();
        selection.extendRule(rule);
        rule.allSourcesAsSlice().makeGreen();
        rule.allTargetsAsSlice().makeGreen();
        for (var filter : filters) {
            filter.extendRule(rule);
        }
        for (var projection : projections) {
            projection.extendRule(rule);
        }
        container.ifPresent(value -> value.extendRule(rule));
        return rule;
    }

    public TripleRule createLinkRule(ResolvedLink link) {
        var rule = new TripleRule();
        rule.setLinkRule(true);
        selection.extendRule(rule);
        link.extendRule(rule);
        return rule;
    }

    @Override
    public String toString() {
        var p = projections.stream().map(Object::toString).collect(Collectors.joining("."));
        var f = filters.stream().map(Object::toString).collect(Collectors.joining("."));
        var c = container.stream().map(Object::toString).collect(Collectors.joining("."));
        var l = links.stream().map(Object::toString).collect(Collectors.joining("."));
        return selection + f + p + c + l;
    }
}
