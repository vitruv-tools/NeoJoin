package tools.vitruv.neojoin.transformation.source;


import static tools.vitruv.neojoin.transformation.ExceptionUtil.executeCatchingTransformatorException;

import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;

import tools.vitruv.neojoin.transformation.InstanceTuple;
import tools.vitruv.neojoin.transformation.TransformatorException;
import tools.vitruv.neojoin.utils.Result;
import tools.vitruv.neojoin.utils.ThrowingBiFunction;
import tools.vitruv.neojoin.utils.ThrowingFunction;

public class SourceUtil {

    public static Function<Result<InstanceTuple, TransformatorException>, Stream<Result<InstanceTuple, TransformatorException>>> joinAndFilterByCondition(
            FromSource rightSource,
            ThrowingBiFunction<InstanceTuple, EObject, Boolean, TransformatorException> condition
    ) {
    return leftResult ->
        leftResult.fold(
            left -> rightSource.getEObjects().flatMap(right -> {
                try {
                    var evaluated = condition.apply(left, right);
                    return Stream.of(new InstanceTuple(left, right))
                            .filter(ignored -> evaluated)
                            .map(Result::of);
                } catch (TransformatorException e) {
                    return Stream.of(Result.fail(e));
                }
            }),
            e -> Stream.of(Result.fail(e))
        );
    }

    public static <T> Function<Result<T, TransformatorException>, Stream<Result<T, TransformatorException>>> evaluateAndFilterByCondition(
            ThrowingFunction<T, Boolean, TransformatorException> evaluate
    ) {
        return (result) -> {
            var evaluationResult = result.bind(it -> Result.executeCatching(TransformatorException.class, () -> evaluate.apply(it)));

            return switch (evaluationResult) {
                case Result.Failure(var failure) -> Stream.of(Result.fail(failure)); // Propagate failure.
                case Result.Success(var value) -> value? Stream.of(result) : Stream.of();
            };
        };
    }
}

