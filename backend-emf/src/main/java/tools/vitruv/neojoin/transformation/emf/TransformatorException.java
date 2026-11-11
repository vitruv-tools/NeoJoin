package tools.vitruv.neojoin.transformation.emf;

import org.eclipse.emf.ecore.EObject;
import org.jspecify.annotations.Nullable;

/**
 * Thrown for user caused errors during transformation. This includes:
 * <ul>
 *     <li>Exceptions during expression evaluation</li>
 *     <li>Instance is contained in multiple other objects</li>
 *     <li>Reference to an instance that is either missing in the target model or mapped multiple times</li>
 * </ul>
 */
public class TransformatorException extends RuntimeException {

    private final @Nullable EObject source;

    public TransformatorException(String message, @Nullable EObject source) {
        super("Failed to transform models: " + message);
        this.source = source;
    }

    public TransformatorException(String message) {
        this(message, null);
    }

    /**
     * Returns the element of the query that caused the error, for example the expression that failed to evaluate.
     * The caller can map it to a location in the query file.
     *
     * @return the element of the query, or {@code null} if the error cannot be attributed to one
     */
    public @Nullable EObject getSourceElement() {
        return source;
    }

}
