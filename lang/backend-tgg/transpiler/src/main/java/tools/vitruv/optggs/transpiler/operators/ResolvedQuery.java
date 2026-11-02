package tools.vitruv.optggs.transpiler.operators;

import tools.vitruv.optggs.transpiler.graph.pattern.GraphPattern;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRulesBuilder;
import tools.vitruv.optggs.transpiler.operators.reference_operators.ResolvedReferenceOperatorChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        var p = projections.stream().map(Object::toString).collect(Collectors.joining("."));
        var f = filters.stream().map(Object::toString).collect(Collectors.joining("."));
        var c = container.stream().map(Object::toString).collect(Collectors.joining("."));
        var l = links.stream().map(Object::toString).collect(Collectors.joining("."));
        return selection + f + p + c + l;
    }

    private record TripleRulesAndConstraints(
            List<TripleRule> rules, List<GraphPattern> constraints) {}
}
