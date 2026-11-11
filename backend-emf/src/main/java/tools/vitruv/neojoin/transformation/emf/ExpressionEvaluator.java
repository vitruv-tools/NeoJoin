package tools.vitruv.neojoin.transformation.emf;

import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.scoping.batch.IFeatureNames;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.common.Result;
import tools.vitruv.neojoin.model.AQRFrom;
import tools.vitruv.neojoin.model.AQRSource;
import tools.vitruv.neojoin.model.ExpressionInterpreter;

import java.util.Iterator;
import java.util.Map;

import static tools.vitruv.neojoin.common.Assertions.check;

/**
 * Wrapper around an {@link ExpressionInterpreter} and a {@link AQRSource} to evaluate feature expressions and conditions.
 */
public class ExpressionEvaluator {

    private final ExpressionInterpreter interpreter;
    private final AQRSource source;
    private final Map<String, Object> parameters;

    public ExpressionEvaluator(ExpressionInterpreter interpreter, AQRSource source) {
        this(interpreter, source, Map.of());
    }

    public ExpressionEvaluator(ExpressionInterpreter interpreter, AQRSource source, Map<String, Object> parameters) {
        this.interpreter = interpreter;
        this.source = source;
        this.parameters = parameters;
    }

    /**
     * Create an evaluation context without any parameters.
     */
    public static Context createContext(ExpressionInterpreter interpreter) {
        return new Context(interpreter, interpreter.createContext());
    }

    /**
     * Create an evaluation context with the given values as parameters.
     *
     * @param values values as parameters for the expression
     * @param limit  see {@link #createContext(Iterator, AQRFrom)}
     */
    public Context createContext(Iterator<?> values, @Nullable AQRFrom limit) {
        var evaluationContext = interpreter.createContext();

        for (var entry : parameters.entrySet()) {
            evaluationContext.newValue(QualifiedName.create(entry.getKey()), entry.getValue());
        }

        var fromIterator = source.allFroms().iterator();
        boolean isFirst = true;
        while (fromIterator.hasNext() && values.hasNext()) {
            var from = fromIterator.next();
            var value = values.next();

            if (isFirst) {
                isFirst = false;
                if (!fromIterator.hasNext()) {
                    evaluationContext.newValue(IFeatureNames.IT, value);
                }
            }

            if (from.alias() != null) {
                evaluationContext.newValue(QualifiedName.create(from.alias()), value);
            }

            if (from == limit) {
                return new Context(interpreter, evaluationContext);
            }
        }

        check(limit == null);
        check(!fromIterator.hasNext() && !values.hasNext());

        return new Context(interpreter, evaluationContext);
    }

    /**
     * Creates an evaluation context with the values from the given instance tuple as parameters.
     *
     * @param instances values as parameters for the expression
     * @param limit     see {@link #createContext(Iterator, AQRFrom)}
     */
    public Context createContext(InstanceTuple instances, @Nullable AQRFrom limit) {
        return createContext(instances.stream().iterator(), limit);
    }

    public static class Context {

        private final ExpressionInterpreter interpreter;
        private final IEvaluationContext evaluationContext;

        public Context(ExpressionInterpreter interpreter, IEvaluationContext evaluationContext) {
            this.interpreter = interpreter;
            this.evaluationContext = evaluationContext;
        }

        /**
         * Evaluate an expression with this context.
         */
        public @Nullable Object evaluateExpression(XExpression expression) {
            var result = interpreter.evaluate(expression, evaluationContext);
            return switch (result) {
                case Result.Success(var value) -> value;
                case Result.Failure(var err) -> throw new TransformatorException(
                    "error during expression evaluation: [%s] %s".formatted(
                        err.getClass().getSimpleName(),
                        err.getMessage()
                    ),
                    expression
                );
            };
        }

        /**
         * Evaluate an expression with this context and check that the result is of type boolean.
         */
        public boolean evaluateCondition(XExpression condition) {
            var result = evaluateExpression(condition);
            if (result instanceof Boolean value) {
                return value;
            } else {
                var actual = result == null ? "null" : result.getClass().getName();
                throw new TransformatorException(
                    "error during condition evaluation: unexpected result type: expected = boolean, actual = " + actual,
                    condition
                );
            }
        }

    }

}
