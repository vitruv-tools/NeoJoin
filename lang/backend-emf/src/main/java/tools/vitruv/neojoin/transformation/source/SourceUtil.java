package tools.vitruv.neojoin.transformation.source;


import static tools.vitruv.neojoin.transformation.ExceptionUtil.executeCatchingTransformatorException;

import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;

import tools.vitruv.neojoin.transformation.InstanceTuple;
import tools.vitruv.neojoin.transformation.TransformatorException;
import tools.vitruv.neojoin.utils.Result;
import tools.vitruv.neojoin.utils.Pair;
import tools.vitruv.neojoin.utils.ThrowingBiFunction;
import tools.vitruv.neojoin.utils.ThrowingFunction;

public class SourceUtil {

    public static Function<Result<InstanceTuple, TransformatorException>, Stream<Result<InstanceTuple, TransformatorException>>> joinAndFilterByCondition(
            FromSource rightSource,
            ThrowingBiFunction<InstanceTuple, EObject, Boolean, TransformatorException> condition
        ) {
        return (leftResult) ->
            switch (leftResult) {
                case Result.Failure<?, TransformatorException> failure -> Stream.of(failure.cast());
                case Result.Success(var left) ->
                    rightSource.getEObjects()
                    .map(right ->
                            executeCatchingTransformatorException(() -> condition.apply(left, right))
                                .map(evaluationResult -> Pair.of(right, evaluationResult)))
                    .filter(result ->
                            switch (result) {
                                case Result.Failure(var failure) -> true; // Do not filter out, in order to propagate failure.
                                case Result.Success(Pair(var ignored, var evaluationResult))  -> (boolean) evaluationResult;
                            })
                    .map(result -> result.map(it ->
                            switch (it) {
                                case Pair(var right, var ignored) -> new InstanceTuple(left, right);
                            }));
            };
    }

    public static <T> Function<Result<T, TransformatorException>, Stream<Result<T, TransformatorException>>> evaluateAndFilterByCondition(
            ThrowingFunction<T, Boolean, TransformatorException> evaluate
    ) {
        return (result) -> {
            var evaluationResult = result.bind(it -> executeCatchingTransformatorException(() -> evaluate.apply(it)));

            return switch (evaluationResult) {
                case Result.Failure(var failure) -> Stream.of(Result.fail(failure)); // Propagate failure.
                case Result.Success(var value) -> value? Stream.of(result) : Stream.of();
            };
        };
    }
}

