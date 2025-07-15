package tools.vitruv.optggs.operators.traits;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.Containment;
import tools.vitruv.optggs.operators.selection.Pattern;
import tools.vitruv.optggs.operators.selection.Union;

public interface Containable<T> {
    T contains(Containment containment);

    FQN defaultSourceElement();

    FQN defaultTargetElement();

    /**
     * Retain all given sources under the given link name for the trailing Metaclass
     * κ(source UNION source ... => A-[link]->?) for σ(...->A => ...)
     *
     * @return itself
     */
    default T contains(Union sources, String link) {
        return contains(new Containment(sources, defaultTargetElement(), link));
    }

    /**
     * Retain given source under the given link name for the trailing Metaclass
     * κ(sources => A-[link]->?) for σ(...->A => ...)
     *
     * @return itself
     */
    default T contains(Pattern source, String link) {
        return contains(new Containment(source, defaultTargetElement(), link));
    }

    /**
     * Retain given element
     * κ(A-[sourceLink]->element => A'-[targetLink]->?) for σ(...->A => ...->A')
     *
     * @return itself
     */
    default T contains(FQN element, String sourceLink, String targetLink) {
        var source = Pattern.from(defaultSourceElement()).ref(element, sourceLink);
        return contains(source, targetLink);
    }

    /**
     * Retain given element
     * κ(A-[link]->element => A'-[link]->?) for σ(...->A => ...->A')
     *
     * @return itself
     */
    default T contains(FQN element, String link) {
        return contains(element, link, link);
    }
}
