package tools.vitruv.neojoin.transformation;

import tools.vitruv.neojoin.utils.Result;
import tools.vitruv.neojoin.utils.ThrowingSupplier;

public class ExceptionUtil {

    private ExceptionUtil() {
    }

    public static final <T> Result<T, TransformatorException> executeCatchingTransformatorException(
            ThrowingSupplier<T, TransformatorException> fn) {
        try {
            return Result.of(fn.supply());
        } catch (TransformatorException e) {
            return Result.fail(e);
        }
    }
}
