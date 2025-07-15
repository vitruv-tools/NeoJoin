package tools.vitruv.optggs.operators.traits;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.Link;
import tools.vitruv.optggs.operators.selection.Pattern;
import tools.vitruv.optggs.operators.selection.Union;

public interface Linkable<T> {
    T references(Link link);

    FQN defaultSourceElement();

    FQN defaultTargetElement();

    /**
     * Retain all given sources under the given link name for the trailing Metaclass
     * λ(source UNION source ... => A-[link]->?) for σ(...->A => ...)
     *
     * @return itself
     */
    default T references(Union sources, String link) {
        return references(new Link(sources, defaultTargetElement(), link));
    }

    /**
     * Retain given source under the given link name for the trailing Metaclass
     * λ(sources => A-[link]->?) for σ(...->A => ...)
     *
     * @return itself
     */
    default T references(Pattern source, String link) {
        return references(new Link(source, defaultTargetElement(), link));
    }

    /**
     * Retain given element
     * λ(A-[sourceLink]->element => A'-[targetLink]->?) for σ(...->A => ...->A')
     *
     * @return itself
     */
    default T references(FQN element, String sourceLink, String targetLink) {
        var source = Pattern.from(defaultSourceElement()).ref(element, sourceLink);
        return references(source, targetLink);
    }

    /**
     * Retain given element
     * λ(A-[link]->element => A'-[link]->?) for σ(...->A => ...->A')
     *
     * @return itself
     */
    default T references(FQN element, String link) {
        return references(element, link, link);
    }
}
