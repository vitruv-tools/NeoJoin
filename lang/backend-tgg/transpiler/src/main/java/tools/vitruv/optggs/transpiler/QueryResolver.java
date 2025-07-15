package tools.vitruv.optggs.transpiler;

import tools.vitruv.optggs.operators.*;
import tools.vitruv.optggs.operators.selection.Pattern;
import tools.vitruv.optggs.operators.selection.PatternLink;
import tools.vitruv.optggs.operators.selection.Union;
import tools.vitruv.optggs.operators.traits.Mappable;

import java.util.*;

public abstract class QueryResolver<V, Q, S, P, F, C extends Mappable, L, PA, PL> {
    public V resolveView(View view) {
        return createView(resolveQueries(view));
    }

    abstract V createView(List<Q> queries);

    private List<Q> resolveQueries(View view) {
        var mappings = view.mappings();
        var containers = findContainers(view);
        var resolvedQueries = new ArrayList<Q>();
        for (var query : view.queries()) {
            var resolvedLinks = query.links().stream().map((link) -> resolveLink(link, mappings)).flatMap(Collection::stream).toList();
            if (containers.containsKey(query.topMapping())) {
                // contained queries
                var container = containers.get(query.topMapping());
                resolvedQueries.add(resolveQuery(query, Optional.of(container), resolvedLinks));
            } else {
                // freestanding queries
                resolvedQueries.add(resolveQuery(query, Optional.empty(), resolvedLinks));
            }

        }
        return resolvedQueries;
    }

    /**
     * Find all containments and resolve them to containers
     * Example:
     * <pre>
     * {
     *     σ(A => A') κ(A-[b]->B => A'-[b]->?) κ(A-[c]-C-[d]->D => A'-[d]->?)
     *     σ(B => B'),
     *     σ(D => D'),
     * }
     * </pre>
     * will yield the following map:
     * <pre>
     * {
     *     (B,B') => Κ(A-[b]->B => A'-[b]->B'),
     *     (D,D') => Κ(A-[c]->C-[d]->D => A'-[d]->D'),
     * }
     * </pre>
     * However, no element can be contained in multiple containers. Therefore, this cannot be resolved and will throw an exception:
     * <pre>
     * {
     *     σ(A => A') κ(A-[b]->B => A'-[b]->?),
     *     σ(B => B'),
     *     σ(C => C') κ(C-[b]->B => C'-[b]->?),
     * }
     * </pre>
     *
     * @return map of mappings to resolved containers
     */
    public Map<Mapping, C> findContainers(View view) {
        var mappings = view.mappings();
        var containers = new HashMap<Mapping, C>();

        for (var query : view.queries()) {
            for (var containment : query.containments()) {
                var tmpContainers = resolveContainment(containment, mappings);
                for (var container : tmpContainers) {
                    var childMapping = container.mapping();
                    if (containers.containsKey(childMapping)) {
                        throw new RuntimeException("Multiple containments for " + childMapping);
                    }
                    containers.put(childMapping, container);
                }
            }
        }

        return containers;
    }

    abstract Q resolveQuery(Query query, Optional<C> containment, List<L> links);

    abstract S resolveSelection(Selection selection);

    abstract P resolveProjection(Projection projection);

    abstract F resolveFilter(Filter filter);

    /**
     * Resolve to resolved containments using the given mappings
     * <br />
     * Example:
     * We have this containment:
     * <pre>
     * κ(A-[b]->B => A'-[b]->?)
     * </pre>
     * and the mappings
     * <pre>
     * { (A,A'), (B,B') }
     * </pre>
     * This is resolved to
     * <pre>
     * Κ(A-[b]->B => A'-[b]->B')
     * </pre>
     * <h2>Unions</h2>
     * A union source is expanded into separate resolved containments. E.g.
     * <pre>
     * κ(A-[b]->B' UNION A-[b]->C" => A'-[b]->?)
     * </pre>
     * together with
     * <pre>
     * { (A,A'), (B',C'), (B",C") }
     * </pre>
     * will yield
     * <pre>
     * {
     *     Κ(A-[b]->B' => A'-[b]->C'),
     *     Κ(A-[b]->B" => A'-[b]->C"),
     * }
     * </pre>
     *
     * @return set of resolved containments
     */
    Collection<C> resolveContainment(Containment containment, Set<Mapping> mappings) {
        var targetParentElement = containment.targetParentElement();
        var targetLink = containment.targetLink();
        var filters = containment.filters().stream().map(this::resolveFilter).toList();
        return containment.sources().resolve(mappings, new Union.Resolver() {
            @Override
            public boolean matches(Mapping mapping, Pattern source) {
                return mapping.source().equals(source.bottom());
            }

            @Override
            public Pattern map(Mapping mapping) {
                return Pattern.from(targetParentElement).ref(mapping.target(), targetLink);
            }
        }).branches().stream().map((branch) -> createContainment(resolvePattern(branch.first()), resolvePattern(branch.last()), filters)).toList();
    }

    /**
     * Resolve to a ResolvedLink with the given mappings
     * <br />
     * Example:
     * We have this link reference:
     * <pre>
     * λ(A-[b]->B => A'-[b]->?)
     * </pre>
     * and the mappings
     * <pre>
     * { (A,A'), (B,B') }
     * </pre>
     * This is resolved to
     * <pre>
     * Λ(A-[b]->B => A'-[b]->B')
     * </pre>
     * <h2>Unions</h2>
     * A union source is expanded into separate resolved links. E.g.
     * <pre>
     * λ(A-[b]->B' UNION A-[b]->C" => A'-[b]->?)
     * </pre>
     * together with
     * <pre>
     * { (A,A'), (B',C'), (B",C") }
     * </pre>
     * will yield
     * <pre>
     * {
     *     Λ(A-[b]->B' => A'-[b]->C'),
     *     Λ(A-[b]->B" => A'-[b]->C"),
     * }
     * </pre>
     *
     * @return list of resolved Links
     */
    Collection<L> resolveLink(Link link, Set<Mapping> mappings) {
        var targetParent = link.targetParent();
        var targetLink = link.targetLink();
        var filters = link.filters().stream().map(this::resolveFilter).toList();
        var resolvedUnion = link.source().resolve(mappings, new Union.Resolver() {
            @Override
            public boolean matches(Mapping mapping, Pattern source) {
                return mapping.source().equals(source.bottom());
            }

            @Override
            public Pattern map(Mapping mapping) {
                return targetParent.ref(mapping.target(), targetLink);
            }
        });
        return resolvedUnion.branches().stream().map((branch) -> createLink(resolvePattern(branch.first()), resolvePattern(branch.last()), filters)).toList();
    }

    abstract C createContainment(PA source, PA target, List<F> filters);

    abstract L createLink(PA source, PA target, List<F> filters);

    public PA resolvePattern(Pattern pattern) {
        var links = pattern.links().stream().map(this::resolvePatternLink).toList();
        return createPattern(links);
    }

    abstract PA createPattern(List<PL> links);

    abstract PL resolvePatternLink(PatternLink patternLink);
}
