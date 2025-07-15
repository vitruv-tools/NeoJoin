package tools.vitruv.optggs.operators;

import tools.vitruv.optggs.operators.selection.Pattern;
import tools.vitruv.optggs.operators.selection.Union;
import tools.vitruv.optggs.operators.traits.Filterable;

import java.util.ArrayList;
import java.util.List;

/**
 * A containment of another element
 * <br />
 * Simple Example:
 * <pre>
 * κ(A-[b]->B => A'-[b]->?)
 * </pre>
 * This keeps all children b of A and maps them as b in A'. We note ? because we do not know to what type B is transformed.
 * Without a separate query that maps B to something, this operator does nothing. This containment operator can only be
 * used in queries that selected an element A previously:
 * <pre>
 * σ(A => A') κ(A-[b]->B => A'-[b]->?)
 * σ(A ⨝(id) C => A') κ(A-[b]->B => A'-[b]->?)
 * σ(C => C') κ(A-[b]->B => A'-[b]->?)
 * </pre>
 * The first two queries are valid. The last query is not valid, because no element A was selected.
 * Usually, the first element in the source and target patterns is omitted; in that case, the link operator is applied
 * to the last element in the selection pattern. Therefore, these queries are equivalent:
 * <pre>
 * σ(A ⨝(id) C => A') κ(-[b]->B => -[b]->?)
 * σ(A ⨝(id) C => A') κ(C-[b]->B => A'-[b]->?)
 * </pre>
 * <h2>Reference patterns</h2>
 * Sometimes, we want to follow multiple references and map only the final element to the target view. Let's look at an
 * example of wheels in a car:
 * <pre>
 * σ(Car => Car) κ(Car-[axes]->Axis-[wheels]->Wheel => Car-[wheels]->?)
 * σ(Wheel => Wheel)
 * </pre>
 * Here, we are not interested in the detail which axis has which wheel attached in the target view. Therefore, we skip
 * over the axes and link all wheels to the car directly.
 * <h2>Unions</h2>
 * Sometimes, different elements are contained by the same containment. This happens, because triple graph grammars do
 * not care about inheritance. Let's assume, we have an abstract class B with two subclasses B' and B". A class A can
 * have children b of superclass B. Therefore, this containment can contain objects of type B' or B". If we transform
 * B' and B" differently (and therefore cannot simply transform their superclass), we cannot write `κ(-[b]->B => -[b]->?).
 * For this case, we can use unions. With unions, we can link to elements with different types (that /usually/ share
 * a supertype; but that is not a requirement). We can therefore write the following containment operation:
 * <pre>
 * κ(-[b]->B' UNION -[b]->B" => -[b]->?)
 * </pre>
 */
public record Containment(Union sources,
                          FQN targetParentElement,
                          String targetLink,
                          List<Filter> filters) implements Filterable<Containment> {


    /**
     * Create a new containment with a union source
     * κ(sources => targetParentElement-[targetLink]->?)
     */
    public Containment(Union sources, FQN targetParentElement, String targetLink) {
        this(sources, targetParentElement, targetLink, new ArrayList<>());
    }

    /**
     * Create a new containment with a single source
     * κ(source => targetParentElement-[targetLink]->?)
     */
    public Containment(Pattern source, FQN targetParentElement, String targetLink) {
        this(new Union(source), targetParentElement, targetLink);
    }

    @Override
    public FQN defaultFilterTarget() {
        var sources = this.sources.sources();
        if (sources.size() != 1) {
            throw new RuntimeException("Cannot apply a filter on a union containment");
        }
        for (var source : sources) {
            return source.bottom();
        }
        throw new RuntimeException("No element in Link source");
    }

    @Override
    public Containment filter(Filter filter) {
        filters.add(filter);
        return this;
    }

    @Override
    public String toString() {
        var f = String.join(", ", filters.stream().map(Object::toString).toList());
        if (!f.isEmpty()) {
            f = " | " + f;
        }
        return "κ(" + sources + " => " + targetParentElement.fqn() + "-[" + targetLink + "]->?" + f + ")";
    }
}
