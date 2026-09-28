package tools.vitruv.neojoin.transformation;

import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.SourceLocation;
import tools.vitruv.neojoin.utils.Result;
import tools.vitruv.neojoin.utils.ThrowingSupplyer;

/**
 * Thrown for user caused errors during transformation. This includes:
 * <ul>
 *     <li>Exceptions during expression evaluation</li>
 *     <li>Instance is contained in multiple other objects</li>
 *     <li>Reference to an instance that is either missing in the target model or mapped multiple times</li>
 * </ul>
 */
public class TransformatorException extends Exception {

    public static final <T> Result<T, TransformatorException> executeCatching(ThrowingSupplyer<T, TransformatorException> fn) {
        try {
            return Result.of(fn.invoke());
        } catch (TransformatorException e) {
            return Result.fail(e);
        }
    }

    private final @Nullable SourceLocation source;

    public TransformatorException(String message, @Nullable SourceLocation source) {
        super("Failed to transform models: " + message);
        this.source = source;
    }

    public TransformatorException(String message) {
        this(message, null);
    }

    public @Nullable SourceLocation getSourceLocation() {
        return source;
    }

}
