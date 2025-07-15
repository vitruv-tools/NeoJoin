package tools.vitruv.optggs.operators;

import tools.vitruv.optggs.operators.selection.Pattern;

import java.util.HashSet;
import java.util.Set;

/**
 * A transformation from a source pattern to a target pattern.
 * <br/>
 * Simple Example:
 * <pre>
 * σ(A => A')
 * </pre>
 * transforms an element A to an element A'.
 * We can do this in code with convenience functions or by constructing object manually:
 * <pre>
 * var query = Query.from(fqn("A"))
 *                  .create(fqn("A'")).build();
 * // or
 * var selection = new Selection(Pattern.from(fqn("A")), Pattern.from(fqn("A'")));
 * var query = new Query(selection);
 * </pre>
 * <h2>Joins</h2>
 * We may have joins in the source or target pattern:
 * <pre>
 * σ(A ⨝(id) B => A')
 * </pre>
 * This selects an element A which has the same id as an element B and transforms them together to an element A'.
 * We can do the same on the target side; in this case, we split a single element into multiple elements:
 * <pre>
 * σ(A => A' ⨝(id) B')
 * </pre>
 * Most operations will be applied to the last element in the pattern. E.g.:
 * <pre>
 * σ(A ⨝(id) B => A') π(name => name) π(A::name => othername)
 * </pre>
 * In this example, the first projection will transform the name of B (which is the last element in the pattern) to
 * name of A'. The second projection explicitly states that we want to use the name of A.
 * <br />
 * We can write the last example with convenience functions:
 * <pre>
 * var query = Query.from(fqn("A")).join(fqn("B"), "id")
 *                  .create(fqn("A'"))
 *                  .project("name")
 *                  .project(fqn("A"), "name", "othername");
 * </pre>
 * Internally, we construct a selection with two patterns for the source selection and target creation. Then, we
 * build a query with this selection and append two projections to it. The first projection does not specify a source
 * element that it is applied to. Therefore, we infer that we mean the last element in the pattern. We construct
 * this query manually:
 * <pre>
 * var selection = new Selection(
 *      Pattern.from(fqn("A")).join(fqn("B"), List.of(new Tuple<>("id", "id"))),
 *      Pattern.from(fqn("A'"))
 * );
 * var query = new Query(selection);
 * query.project(new SimpleProjection(Pattern.from(fqn("B")), fqn("A'"), "name", "name"));
 * query.project(new SimpleProjection(Pattern.from(fqn("A")), fqn("A'"), "name", "othername"));
 * </pre>
 * <h2>Transforming strongly connected elements together</h2>
 * Sometimes, we need to transform strongly connected elements as one element. In this case, we can use reference patterns:
 * <pre>
 * σ(A-[a]->B => A')
 * </pre>
 * This transforms only elements A that have a reference b to an element B into a single element A'. This possibility is
 * primarily used internally, when resolving interdependent queries. However, some complex queries may use this as well.
 * Keep in mind, that each element is only transformed once. Therefore, an element A must have exactly one reference to
 * an object B. Without any reference, an element A is not transformed. With multiple references to B, only one reference
 * (and therefore only one element B) is transformed.
 * You can use reference patterns, if you need to get attributes from referenced elements. E.g.:
 * <pre>
 * σ(A-[b]->B => A') π(B::id => id)
 * </pre>
 * This will set the attribute id of A' to the attribute id of the referenced B. This also requires that A will
 * reference exactly one B.
 * To do this in code, we use the `ref` function for queries or patterns:
 * <pre>
 * var query = Query.from(fqn("A")).ref(fqn("B"), "b")
 *                  .create(fqn("A'")).build()
 *                  .project("id");
 * </pre>
 */
public record Selection(Pattern source, Pattern target) {

    /**
     * Get all mappings that this selection creates
     * Example:
     * <pre>
     * σ(A ⨝(id) B-[c]->C => A'-[b]->B')
     * </pre>
     * will yield these mappings:
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

    /**
     * Get only the outer mappings that this selection creates
     * Example:
     * <pre>
     * σ(A ⨝(id) B-[c]->C => A'-[b]->B')
     * </pre>
     * will yield these mappings:
     * <pre>
     * { (A,A'), (A,B'), (C,A'), (C,B') }
     * </pre>
     *
     * @return set of mappings
     */
    public Set<Mapping> primaryMappings() {
        var mappings = new HashSet<Mapping>();
        mappings.add(new Mapping(source.top(), target.top()));
        mappings.add(new Mapping(source.top(), target.bottom()));
        mappings.add(new Mapping(source.bottom(), target.top()));
        mappings.add(new Mapping(source.bottom(), target.bottom()));
        return mappings;
    }

    /**
     * Get only the mapping at the top of the patterns
     * Example:
     * <pre>
     * σ(A ⨝(id) B-[c]->C => A'-[b]->B')
     * </pre>
     * will yield this mapping
     * <pre>
     * (A,A')
     * </pre>
     *
     * @return mapping
     */
    public Mapping topMapping() {
        return new Mapping(source.top(), target.top());
    }

    @Override
    public String toString() {
        return "σ(" + source + " => " + target + ")";
    }
}
