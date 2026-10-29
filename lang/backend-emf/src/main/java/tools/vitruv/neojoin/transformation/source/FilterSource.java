package tools.vitruv.neojoin.transformation.source;

import static tools.vitruv.neojoin.transformation.source.SourceUtil.evaluateAndFilterByCondition;

import java.util.stream.Stream;

import org.eclipse.xtext.xbase.XExpression;

import tools.vitruv.neojoin.transformation.ExpressionEvaluator;
import tools.vitruv.neojoin.transformation.InstanceTuple;
import tools.vitruv.neojoin.transformation.TransformatorException;
import tools.vitruv.neojoin.utils.Result;

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
    public Stream<Result<InstanceTuple, TransformatorException>> get() {
        return inner.get()
            .flatMap(evaluateAndFilterByCondition(tuple ->
                        evaluator.createContext(tuple, null).evaluateCondition(expression)));
    }

}
