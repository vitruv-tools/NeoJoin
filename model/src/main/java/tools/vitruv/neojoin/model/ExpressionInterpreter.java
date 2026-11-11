package tools.vitruv.neojoin.model;

import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.scoping.batch.IFeatureNames;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.common.Result;

/**
 * Evaluates the {@link XExpression Xbase expressions} of an {@link AQR}, for example the feature expressions and
 * conditions of a query. The language frontend provides the implementation, because the expressions can only be
 * evaluated in the context of the resources and type information that the frontend builds. Consumers such as
 * transformation backends depend only on this interface.
 * <p>
 * The variables of an expression are the aliases of the {@link AQRFrom sources} of its target class, the
 * {@link AQRParameter query parameters}, and {@link IFeatureNames#IT} for the only source of a class without
 * joins, for example {@code it} in {@code from Pizza create Food { name := it.name }}.
 */
public interface ExpressionInterpreter {

    /**
     * Creates an empty evaluation context with no accessible variables.
     *
     * @return evaluation context to be used in {@link #evaluate(XExpression, IEvaluationContext) evaluate(..)}
     */
    IEvaluationContext createContext();

    /**
     * Evaluates the given expression with the given context.
     *
     * @param expression expression to evaluate
     * @param context    context, i.e. accessible local variables
     * @return {@link Result.Success} with the returned value if execution was successful,
     *     or {@link Result.Failure} with the thrown exception otherwise
     */
    Result<@Nullable Object> evaluate(XExpression expression, IEvaluationContext context);

}
