package tools.vitruv.neojoin.transformation.source;

import org.eclipse.xtext.xbase.XExpression;
import tools.vitruv.neojoin.transformation.ExpressionEvaluator;
import tools.vitruv.neojoin.transformation.InstanceTuple;
import tools.vitruv.neojoin.transformation.TransformatorException;

import java.util.stream.Stream;

/**
 * Filters objects from the given instance source with the given expression.
 */
public class FilterSource implements InstanceSource {

    private final XExpression expression;
    private final InstanceSource inner;
    private final ExpressionEvaluator evaluator;

    public FilterSource(XExpression expression, InstanceSource inner, ExpressionEvaluator evaluator) {
        this.expression = expression;
        this.inner = inner;
        this.evaluator = evaluator;
    }

    @Override
    public Stream<InstanceTuple> get() {
        return inner.get()
            .filter(tuple -> {
                try {
                    return evaluator.createContext(tuple, null).evaluateCondition(expression);
                } catch (TransformatorException e) {
                    // TODO: handle properly
                    throw new RuntimeException(e);
                }
            });
    }

}
