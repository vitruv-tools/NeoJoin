package tools.vitruv.optggs.operators.traits;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.FunctionInvocation;
import tools.vitruv.optggs.operators.Projection;
import tools.vitruv.optggs.operators.projections.DerivedProjection;
import tools.vitruv.optggs.operators.projections.SimpleProjection;
import tools.vitruv.optggs.operators.selection.Pattern;

public interface Projectable<T> {
    FQN defaultSourceElement();

    FQN defaultTargetElement();

    T project(Projection projection);

    /**
     * Project sourceProperty to targetProperty for the trailing Metaclasses π(A::sourceProperty =>
     * A'::targetProperty) for σ(A => A') or σ(...->A => ...->A')
     *
     * @return itself
     */
    default T project(String sourceProperty, String targetProperty) {
        var source = Pattern.from(defaultSourceElement());
        var target = defaultTargetElement();
        var projection = new SimpleProjection(source, target, sourceProperty, targetProperty);
        return project(projection);
    }

    /**
     * Project property for the trailing Metaclasses π(A::property => A'::property) for σ(A => A')
     * or σ(...->A => ...->A')
     *
     * @return itself
     */
    default T project(String property) {
        return project(property, property);
    }

    /**
     * Project sourceProperty of given element to targetProperty of the trailing target metaclass
     * π(element::sourceProperty => A'::targetProperty) for σ(...->element-...->A => ...->A')
     *
     * @return itself
     */
    default T project(FQN element, String sourceProperty, String targetProperty) {
        var target = defaultTargetElement();
        var projection =
                new SimpleProjection(Pattern.from(element), target, sourceProperty, targetProperty);
        return project(projection);
    }

    /**
     * Project property of given element to the trailing target metaclass π(element::property =>
     * A'::property) for σ(...->element-...->A => ...->A')
     *
     * @return itself
     */
    default T project(FQN element, String property) {
        return project(element, property, property);
    }

    /**
     * Create targetProperty on trailing target metaclass according to the given function
     * π(A'::targetProperty = function(...)) for σ(... => ...->A')
     *
     * @return itself
     */
    default T project(FunctionInvocation function, String targetProperty) {
        function.setConstrainedArgument("return", defaultTargetElement(), targetProperty);
        return project(new DerivedProjection(function));
    }
}
