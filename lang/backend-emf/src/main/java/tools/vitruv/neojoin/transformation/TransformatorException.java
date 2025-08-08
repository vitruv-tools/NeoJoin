package tools.vitruv.neojoin.transformation;

import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.SourceLocation;

/**
 * Thrown for user caused errors during transformation. This includes:
 * <ul>
 *     <li>Exceptions during expression evaluation</li>
 *     <li>Instance is contained in multiple other objects</li>
 *     <li>Reference to an instance that is either missing in the target model or mapped multiple times</li>
 * </ul>
 */
public class TransformatorException extends RuntimeException {

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
