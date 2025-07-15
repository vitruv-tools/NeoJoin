package tools.vitruv.optggs.operators;

import tools.vitruv.optggs.operators.builder.SourceSelectionBuilder;
import tools.vitruv.optggs.operators.traits.Containable;
import tools.vitruv.optggs.operators.traits.Filterable;
import tools.vitruv.optggs.operators.traits.Linkable;
import tools.vitruv.optggs.operators.traits.Projectable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A single query consisting of multiple operators
 * Conceptually, a query is made up of:
 * <pre>
 * SELECTION (FILTER)* (PROJECTION)* (CONTAINMENT)* (LINK)*
 * with SELECTION = FROM-CLAUSE CREATE-CLAUSE
 * </pre>
 * The FROM-CLAUSE and CREATE-CLAUSE each consist of a selection pattern:
 * <pre>
 * PATTERN = FROM (JOIN | REF)*
 * </pre>
 * <p>
 * Example:
 * <pre>
 * from A-[b]->B join C on id
 * create A'
 * filter A::id > 5
 * project id := A::id
 * contains d := A-[d]->D
 * references e := B-[e]->E
 * </pre>
 * In this example, Metaclasses A and B are joined with C and transformed together to Metaclass A' when A's id is greater than 5.
 * The id, as well as all containments of classes D are retained. All weak references from B to E are transformed to
 * weak references from A' to E' (if E is transformed to E' in a separate query).
 * In operator language, we write:
 * <pre>
 * σ(A-[b]->B ⨝(id) C => A') φ(A::id > 5) π(A::id => A'::id) κ(A-[d]->D => A'->[d]->?) λ(B-[e]->E => A'-[e]->?)
 * </pre>
 * We can code this example as following:
 * <pre>
 * Query.from(fqn("A")).ref(fqn("B"), "b").join(fqn("C"), "id")
 *      .create(fqn("A'")).build()
 *      .filter("id", LogicOperator.MoreThan, ConstantExpression.primitive(5))
 *      .project("id")
 *      .contains(fqn("D"), "d")
 *      .references(fqn("E"), "e");
 * </pre>
 */
public record Query(Selection selection,
                    List<Projection> projections,
                    List<Filter> filters,
                    List<Containment> containments,
                    List<Link> links) implements Filterable<Query>, Projectable<Query>, Containable<Query>, Linkable<Query> {

    /**
     * New Query for a Selection
     *
     * @param selection Selection clause
     */
    public Query(Selection selection) {
        this(selection, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

    }

    /**
     * New Query for a From clause
     * σ(element ...)
     *
     * @return builder for source selection
     */
    public static SourceSelectionBuilder from(FQN element) {
        return new SourceSelectionBuilder(element);
    }

    public Query project(Projection projection) {
        projections.add(projection);
        return this;
    }

    @Override
    public Query filter(Filter filter) {
        filters.add(filter);
        return this;
    }

    @Override
    public Query contains(Containment containment) {
        containments.add(containment);
        return this;
    }

    @Override
    public Query references(Link link) {
        links.add(link);
        return this;
    }

    @Override
    public FQN defaultSourceElement() {
        return selection.source().bottom();
    }

    @Override
    public FQN defaultTargetElement() {
        return selection.target().bottom();
    }

    @Override
    public FQN defaultFilterTarget() {
        return selection.source().bottom();
    }

    /**
     * Get all mappings in the selection
     * e.g. for σ(A-[b]->B-[c]->C => A'-[d]->D') you get mappings {(A,A'), (A,D'), (B, A'), (B, D'), (C, A'), (C, D')}
     *
     * @return set of Mappings
     */
    public Set<Mapping> mappings() {
        return selection.mappings();
    }

    /**
     * Get only outmost mappings in selection
     * e.g. for σ(A-[b]->B-[c]->C => A'-[d]->D') you get mappings {(A,A'), (A,D'), (C, A'), (C, D')}
     *
     * @return set of mappings
     */
    public Set<Mapping> primaryMappings() {
        return selection.primaryMappings();
    }

    /**
     * Get the mapping of the element at the top of the selection pattern
     * e.g. for σ(A-[b]->B-[c]->C => A'-[d]->D') you get mappings (A,A')
     *
     * @return Mapping
     */
    public Mapping topMapping() {
        return selection.topMapping();
    }


    @Override
    public String toString() {
        var p = String.join(".", projections.stream().map(Object::toString).toList());
        var f = String.join(".", filters.stream().map(Object::toString).toList());
        var c = String.join(".", containments.stream().map(Object::toString).toList());
        var l = String.join(".", links.stream().map(Object::toString).toList());
        return selection + f + p + c + l;
    }
}
