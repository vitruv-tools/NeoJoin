package tools.vitruv.neojoin.transformation.source;

import org.eclipse.xtext.xbase.XExpression;
import tools.vitruv.neojoin.transformation.ExpressionEvaluator;
import tools.vitruv.neojoin.transformation.InstanceTuple;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Filters objects from the given instance source with the given expression.
 */
public class FilterSource implements InstanceSource {

    private final XExpression expression;
    private final InstanceSource inner;
    private final ExpressionEvaluator evaluator;
    private final Map<String, Object> parameters;

    public FilterSource(XExpression expression, InstanceSource inner, ExpressionEvaluator evaluator, Map<String, Object> parameters) {
        this.expression = expression;
        this.inner = inner;
        this.evaluator = evaluator;
        this.parameters = parameters;
    }

    @Override
    public Stream<InstanceTuple> get() {
        if (parameters.values().stream().anyMatch(Objects::isNull)) {
            return inner.get();
        }
        return inner.get()
            .filter(tuple -> evaluator.createContext(tuple, null).evaluateCondition(expression));
    }

}
