package tools.vitruv.optggs.operators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A view consisting of multiple interdependent queries
 * Example:
 * <pre>
 * {
 *     σ(A => A') κ(A-[b]->B => A'-[b]->?) λ(A-[c]->C => A'-[x]->?),
 *     σ(B => B'),
 *     σ(C => C'),
 * }
 * </pre>
 * This is a view consisting of three interdependent queries.
 * The first query selects metaclass A as A' and retains all contained children b of type B as children b of whatever type B is transformed to.
 * Additionally, all links c to objects of type C are retained as links x.
 * The second query selects metaclass B as B' and the third query selects metaclasss C as C'.
 * Due to the containments and links, these queries are interdependent.
 * <h2>Resolve interdependent queries</h2>
 * To resolve any dependencies, we can use the `resolveQueries()` function. This creates the following resolved view:
 * <pre>
 * {
 *     σ(A => A') Λ(A-[c]->C => A'-[x]->C'),
 *     σ(B => B') Κ(A-[b]->B => A-[b]->B'),
 *     σ(C => C'),
 * }
 * </pre>
 * Note that in these queries, the missing classes that B and C are transformed to are resolved in the Κ and Λ operators.
 * Furthermore, the κ operator was moved to the second query. The containment operator κ tells us, that the currently
 * selected element should *contain* the referenced element. However, in TGGs this containment would be expressed as a
 * *required* context, thus making it a *contained by* rather than a *contains* requirement. This *contained by* nature is
 * reflected by the resolved containment operator Κ, which moves to the query dealing with the contained element.
 * <h2>Transform to Triple Grammar</h2>
 * Finally, the resolve view needs to be transformed to a triple grammar. Both resolving and transforming to a triple grammar
 * is done with the `toGrammar(name)` method. The view above would be transformed to triple grammar similar to this:
 * <pre>
 * tripleGrammar {
 *     ...
 *     correspondences{
 *         A <- AToA' -> A'
 *         B <- BToB' -> B'
 *         C <- CToC' -> C'
 *     }
 * }
 * tripleRule SelectAAsA' {
 *     source {
 *         ++ a: A {}
 *     }
 *     target {
 *         ++ a': A' {}
 *     }
 *     correspondences {
 *         ++ a <- :AToA' -> a'
 *     }
 * }
 * tripleRule SelectBAsB' {
 *     source {
 *         a: A { ++ -b->b }
 *         ++ b: B {}
 *     }
 *     target {
 *         a': A' { ++ -b->b }
 *         ++ b': B {}
 *     }
 *     correspondences {
 *         a <- AToA' -> a'
 *         ++ b <- BToB' -> b'
 *     }
 * }
 * tripleRule SelectCAsC' {
 *     source {
 *         ++ c: C {}
 *     }
 *     target {
 *         ++ c': C' {}
 *     }
 *     correspondences {
 *         ++ c <- :CToC' -> c'
 *     }
 * }
 * tripleRule LinkAToC {
 *     source {
 *         a: A { ++ -c->c }
 *         c: C {}
 *     }
 *     target {
 *         a': A' { ++ -x->c' }
 *         c': C' {}
 *     }
 *     correspondences {
 *         a <- :AToA' -> a'
 *         c <- :CToC' -> c'
 *     }
 * }
 * </pre>
 * Note that each query is transformed to one triple rule that transforms the selected element. However, the first query
 * is transformed to a second rule, the `LinkAToC` rule. This rule expects a tuple of already transformed elements A and C
 * and transforms the link c between them to a link x. Note that the link is transformed independently of the elements to
 * ensure that multiple links between elements of the same type can be transformed. This is different to containments,
 * which are included in the query and rule (see rule SelectBAsB'). In this case, elements B are only transformed when
 * they are contained by an element A, which was already transformed. Non-contained elements B are never transformed.
 * <br />
 * We can use a view as following:
 * <pre>
 * var view = new View();
 * view.addQuery(...);
 * // get triple grammar:
 * var grammar = view.toGrammar("MyGrammar");
 * // or, if we want to use postprocess the resolved queries:
 * var queries = view.resolveQueries();
 * </pre>
 */
public record View(List<Query> queries) {

    public View() {
        this(new ArrayList<>());
    }

    public void addQuery(Query query) {
        queries.add(query);
    }

    /**
     * Get the mappings of all queries in this view
     * Example:
     * <pre>
     * {
     *     σ(A => A'),
     *     σ(B-[c]->C => C'),
     * }
     * </pre>
     * would return the following mappings:
     * <pre>
     * {
     *     (A,A'),
     *     (B,C'), (C,C'),
     * }
     * </pre>
     *
     * @return set of mappings
     */
    public Set<Mapping> mappings() {
        var mappings = new HashSet<Mapping>();
        for (var query : queries) {
            mappings.addAll(query.mappings());
        }
        return mappings;
    }

    @Override
    public String toString() {
        return queries.toString();
    }
}
