package tools.vitruv.optggs.transpiler.operators;

import tools.vitruv.optggs.transpiler.graph.pattern.GraphPattern;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRulesBuilder;
import tools.vitruv.optggs.transpiler.operators.reference_operators.ResolvedReferenceOperatorChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ResolvedQuery {
    private final ResolvedSelection selection;
    private final List<ResolvedProjection> projections;
    private final List<ResolvedReferenceOperatorChain> referenceOperatorChains;
    private final List<ResolvedFilter> filters;
    private final Optional<ResolvedContainment> container;
    private final List<ResolvedLink> links;

    private final List<TripleRule> generatedRules;

    private final List<GraphPattern> generatedConstraints;

    public ResolvedQuery(
            ResolvedSelection selection,
            List<ResolvedProjection> projections,
            List<ResolvedReferenceOperatorChain> referenceOperatorChains,
            List<ResolvedFilter> filters,
            Optional<ResolvedContainment> container,
            List<ResolvedLink> links) {
        this.selection = selection;
        this.projections = projections;
        this.referenceOperatorChains = referenceOperatorChains;
        this.filters = filters;
        this.container = container;
        this.links = links;

        // Generate rules and constraints
        final TripleRule primaryRule = createPrimaryRule();
        final List<TripleRule> linkRules = links.stream().map(this::createLinkRule).toList();
        final TripleRulesAndConstraints referenceOperatorRulesAndConstraints =
                createRulesAndConstraintsForReferenceOperators();

        final List<TripleRule> allGeneratedRules = new ArrayList<>();
        allGeneratedRules.add(primaryRule);
        allGeneratedRules.addAll(linkRules);
        allGeneratedRules.addAll(referenceOperatorRulesAndConstraints.rules());

        this.generatedRules = allGeneratedRules;
        this.generatedConstraints = referenceOperatorRulesAndConstraints.constraints();
    }

    public List<TripleRule> getGeneratedRules() {
        return generatedRules;
    }

    public List<GraphPattern> getGeneratedConstraints() {
        return generatedConstraints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResolvedQuery other)) return false;
        return Objects.equals(selection, other.selection)
                && Objects.equals(projections, other.projections)
                && Objects.equals(referenceOperatorChains, other.referenceOperatorChains)
                && Objects.equals(filters, other.filters)
                && Objects.equals(container, other.container)
                && Objects.equals(links, other.links)
                && Objects.equals(generatedRules, other.generatedRules)
                && Objects.equals(generatedConstraints, other.generatedConstraints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                selection,
                projections,
                referenceOperatorChains,
                filters,
                container,
                links,
                generatedRules,
                generatedConstraints);
    }

    private TripleRule createPrimaryRule() {
        final TripleRule rule = new TripleRule();
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

    private TripleRule createLinkRule(ResolvedLink link) {
        final TripleRule rule = new TripleRule();
        selection.extendRule(rule);
        link.extendRule(rule);
        return rule;
    }

    private TripleRulesAndConstraints createRulesAndConstraintsForReferenceOperators() {
        final List<TripleRulesBuilder> referenceOperatorRulesAndConstraints =
                referenceOperatorChains.stream()
                        .map(ResolvedReferenceOperatorChain::generateRulesAndConstraints)
                        .toList();
        final List<TripleRule> referenceOperatorRules =
                referenceOperatorRulesAndConstraints.stream()
                        .map(TripleRulesBuilder::getTripleRules)
                        .flatMap(List::stream)
                        .toList();
        final List<GraphPattern> referenceOperatorConstraints =
                referenceOperatorRulesAndConstraints.stream()
                        .map(TripleRulesBuilder::getConstraints)
                        .flatMap(List::stream)
                        .toList();

        return new TripleRulesAndConstraints(referenceOperatorRules, referenceOperatorConstraints);
    }

    @Override
    public String toString() {
        var p = String.join(".", projections.stream().map(Object::toString).toList());
        var f = String.join(".", filters.stream().map(Object::toString).toList());
        var c = String.join(".", container.stream().map(Object::toString).toList());
        var l = String.join(".", links.stream().map(Object::toString).toList());
        return selection + f + p + c + l;
    }

    private record TripleRulesAndConstraints(
            List<TripleRule> rules, List<GraphPattern> constraints) {}
}
