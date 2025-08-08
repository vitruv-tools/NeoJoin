package tools.vitruv.neojoin.transformation;

import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.SourceLocation;
import tools.vitruv.neojoin.aqr.AQRFrom;
import tools.vitruv.neojoin.aqr.AQRSource;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.Result;

import java.util.Iterator;

/**
 * Wrapper around an {@link ExpressionHelper} and a {@link AQRSource} to evaluate feature expressions and conditions.
 */
public class ExpressionEvaluator {

    private final ExpressionHelper helper;
    private final AQRSource source;

    public ExpressionEvaluator(ExpressionHelper helper, AQRSource source) {
        this.helper = helper;
        this.source = source;
    }

    /**
     * Create an evaluation context without any parameters.
     */
    public static Context createContext(ExpressionHelper helper) {
        return new Context(helper, helper.createContext());
    }

    /**
     * Create an evaluation context with the given values as parameters.
     *
     * @param values values as parameters for the expression
     * @param limit  see {@link ExpressionHelper#createContext(Iterator, Iterator, AQRFrom)}
     */
    public Context createContext(Iterator<?> values, @Nullable AQRFrom limit) {
        var evaluationContext = helper.createContext(source.allFroms().iterator(), values, limit);
        return new Context(helper, evaluationContext);
    }

    /**
     * Creates an evaluation context with the values from the given instance tuple as parameters.
     *
     * @param instances values as parameters for the expression
     * @param limit     see {@link ExpressionHelper#createContext(Iterator, Iterator, AQRFrom)}
     */
    public Context createContext(InstanceTuple instances, @Nullable AQRFrom limit) {
        return createContext(instances.stream().iterator(), limit);
    }

    public static class Context {

        private final ExpressionHelper helper;
        private final IEvaluationContext evaluationContext;

        public Context(ExpressionHelper helper, IEvaluationContext evaluationContext) {
            this.helper = helper;
            this.evaluationContext = evaluationContext;
        }

        /**
         * Evaluate an expression with this context.
         */
        public @Nullable Object evaluateExpression(XExpression expression) {
            var result = helper.evaluate(expression, evaluationContext);
            return switch (result) {
                case Result.Success<?>(var value) -> value;
                case Result.Failure<?>(var err) -> throw new TransformatorException(
                    "error during expression evaluation: [%s] %s".formatted(
                        err.getClass().getSimpleName(),
                        err.getMessage()
                    ),
                    SourceLocation.from(expression)
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
                    SourceLocation.from(condition)
                );
            }
        }

    }

}
