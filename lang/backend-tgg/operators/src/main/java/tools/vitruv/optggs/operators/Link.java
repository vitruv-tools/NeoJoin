package tools.vitruv.optggs.operators;

import tools.vitruv.optggs.operators.selection.Pattern;
import tools.vitruv.optggs.operators.selection.Union;
import tools.vitruv.optggs.operators.traits.Filterable;

import java.util.ArrayList;
import java.util.List;

/**
 * A reference from one element to another
 * <br />
 * Simple example:
 * <pre>
 * λ(A-[b]->B => A'-[b]->?)
 * </pre>
 * This maps references b from A to B to references b from A' to whatever B is transformed to.
 * This link operator can only be used in queries that selected an element A previously:
 * <pre>
 * σ(A => A') λ(A-[b]->B => A'-[b]->?)
 * σ(A ⨝(id) C => A') λ(A-[b]->B => A'-[b]->?)
 * σ(C => C') λ(A-[b]->B => A'-[b]->?)
 * </pre>
 * The first two queries are valid. The last query is not valid, because no element A was selected.
 * Usually, the first element in the source and target patterns is omitted; in that case, the link operator is applied
 * to the last element in the selection pattern. Therefore, these queries are equivalent:
 * <pre>
 * σ(A ⨝(id) C => A') λ(-[b]->B => -[b]->?)
 * σ(A ⨝(id) C => A') λ(C-[b]->B => A'-[b]->?)
 * </pre>
 * In code, you can use convenience functions to construct link references:
 * <pre>
 * Query.from(fqn("A")).join(fqn("C"), "id")
 *      .create(fqn("A'")).build()
 *      .references(fqn("B"), "b");
 * // or
 * var query = Query.from(fqn("A")).join(fqn("C"), "id")
 *                  .create(fqn("A'")).build();
 * var link = new Link(Pattern.from("C").ref(fqn("B"), "b"), Pattern.from(fqn("A'")), "b");
 * query.references(link);
 * </pre>
 * <h2>Reference patterns</h2>
 * Sometimes, we want to follow multiple references and map only the final element to the target view. Let's look at an
 * example of wheels in a car:
 * <pre>
 * σ(Car => Car) λ(Car-[axes]->Axis-[wheels]->Wheel => Car-[wheels]->?)
 * σ(Wheel => Wheel)
 * </pre>
 * Here, we are not interested in the detail which axis has which wheel attached in the target view. Therefore, we skip
 * over the axes and link all wheels to the car directly.
 * In code, we need to construct this link explicitly:
 * <pre>
 * var link = new Link(Pattern.from(fqn("Car")).ref(fqn("Axis"), "axes").ref(fqn("Wheel"), "wheels"), fqn("Car"), "wheels");
 * </pre>
 * <h2>Create target elements</h2>
 * For some queries, it might be necessary to create a target element for a simple reference. Think about elements that
 * hold some information about the reference itself. The following example is a hyperlink between two pages:
 * <pre>
 * σ(Page => Page) λ(Page-[link]->Page => Page-[outgoing]->Hyperlink-[target]->?)
 * </pre>
 * A simple reference between two `Page` objects is mapped to a reference pattern from a `Page` to a `Hyperlink` object
 * and then to the original target page. A restriction is, that the intermediate target elements (here the `Hyperlink`)
 * cannot have any projected properties or references themselves.
 * <h2>Unions</h2>
 * Sometimes, different elements are referenced by the same reference. This happens, because triple graphs grammars do
 * not care about inheritance. Let's assume, we have an abstract class B with two subclasses B' and B". A class A can
 * have a reference b to superclass B. Therefore, this reference can point to objects of type B' or B". If we transform
 * B' and B" differently (and therefore cannot simply transform their superclass), we cannot write `λ(-[b]->B => -[b]->?).
 * For this case, we can use unions. With unions, we can link to elements with different types (that /usually/ share
 * a supertype; but that is not a requirement). We can therefore write the following link operation:
 * <pre>
 * λ(-[b]->B' UNION -[b]->B" => -[b]->?)
 * </pre>
 */
public record Link(Union source,
                   Pattern targetParent,
                   String targetLink,
                   List<Filter> filters) implements Filterable<Link> {

    /**
     * Create a new link reference with a union source and a complex target parent
     * λ(source => targetParent-[targetLink]->?)
     * <br />
     * Example:
     * <pre>
     * λ(A-[b]->B' UNION A-[b]->B" => A'-[c]->C-[b]->?)
     * </pre>
     * is created with:
     * <pre>
     * var first = Pattern.from(fqn("A")).ref(fqn("B'"), "b");
     * var second = Pattern.from(fqn("A")).ref(fqn("B\""), "b");
     * var union = new Union(first).add(second);
     * var targetParent = Pattern.from(fqn("A'").ref(fqn("C"), "c");
     * new Link(union, targetParent, "b");
     * </pre>
     */
    public Link(Union source, Pattern targetParent, String targetLink) {
        this(source, targetParent, targetLink, new ArrayList<>());
    }

    /**
     * Create a new link reference with a union source and a single target parent
     * λ(source => targetParent-[targetLink]->?)
     * <br />
     * Example:
     * <pre>
     * λ(A-[b]->B' UNION A-[b]->B" => A'-[b]->?)
     * </pre>
     * is created with:
     * <pre>
     * var first = Pattern.from(fqn("A")).ref(fqn("B'"), "b");
     * var second = Pattern.from(fqn("A")).ref(fqn("B\""), "b");
     * var union = new Union(first).add(second);
     * new Link(union, fqn("A'"), "b");
     * </pre>
     */
    public Link(Union source, FQN targetParentElement, String targetLink) {
        this(source, Pattern.from(targetParentElement), targetLink);
    }

    /**
     * Create a new link reference with a single source and a complex target parent
     * λ(source => targetParent-[targetLink]->?)
     * <br />
     * Example:
     * <pre>
     * λ(A-[b]->B => A'-[c]->C-[b]->?)
     * </pre>
     * is created with:
     * <pre>
     * var source = Pattern.from(fqn("A")).ref(fqn("B"), "b");
     * var targetParent = Pattern.from(fqn("A'").ref(fqn("C"), "c");
     * new Link(source, targetParent, "b");
     * </pre>
     */
    public Link(Pattern source, Pattern targetParent, String targetLink) {
        this(new Union(source), targetParent, targetLink);
    }

    /**
     * Create a new link reference with a single source and a single target parent
     * λ(source => targetParent-[targetLink]->?)
     * <br />
     * Example:
     * <pre>
     * λ(A-[b]->B => A'-[b]->?)
     * </pre>
     * is created with:
     * <pre>
     * var source = Pattern.from(fqn("A")).ref(fqn("B"), "b");
     * new Link(source, fqn("A'"), "b");
     * </pre>
     */
    public Link(Pattern source, FQN targetParentElement, String targetLink) {
        this(new Union(source), targetParentElement, targetLink);
    }

    @Override
    public FQN defaultFilterTarget() {
        var sources = source.sources();
        if (sources.size() != 1) {
            throw new RuntimeException("Cannot apply a filter on a union link");
        }
        for (var source : sources) {
            return source.bottom();
        }
        throw new RuntimeException("No element in Link source");
    }

    @Override
    public Link filter(Filter filter) {
        filters.add(filter);
        return this;
    }

    @Override
    public String toString() {
        var f = String.join(", ", filters.stream().map(Object::toString).toList());
        if (!f.isEmpty()) {
            f = " | " + f;
        }
        return "λ(" + source + " => " + targetParent + "-[" + targetLink + "]->?" + f + ")";
    }
}
