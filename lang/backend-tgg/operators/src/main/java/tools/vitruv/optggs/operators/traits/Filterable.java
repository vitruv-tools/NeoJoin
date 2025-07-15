package tools.vitruv.optggs.operators.traits;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.Filter;
import tools.vitruv.optggs.operators.filters.ConstantFilter;

public interface Filterable<T> {
    FQN defaultFilterTarget();
    T filter(Filter filter);
    /**
     * Filter element for given property, operator and value
     * φ(element::property operator value)
     * e.g. φ(A::prop < 5)
     *
     * @return itself
     */
    default T filter(FQN element, String property, LogicOperator operator, ConstantExpression value) {
        return filter(new ConstantFilter(element, property, operator, value));
    }

    /**
     * Filter leading source metaclass for given property, operator and value
     * φ(A::property operator value) for σ(A-... => ...)
     * e.g. φ(A::prop < 5)
     *
     * @return itself
     */
    default T filter(String property, LogicOperator operator, ConstantExpression value) {
        return filter(defaultFilterTarget(), property, operator, value);
    }

    /**
     * Filter leading source metaclass for given property and value
     * φ(A::property == value) for σ(A-... => ...)
     * e.g. φ(A::prop == 5)
     *
     * @return itself
     */
    default T filter(String property, ConstantExpression value) {
        return filter(defaultFilterTarget(), property, LogicOperator.Equals, value);
    }
}
