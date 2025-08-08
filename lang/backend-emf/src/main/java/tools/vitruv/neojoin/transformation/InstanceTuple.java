package tools.vitruv.neojoin.transformation;

import org.eclipse.emf.ecore.EObject;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * A tuple of instances that results for example from joining multiple classes in a query.
 * Use {@link #stream()} to retrieve the contained instances.
 *
 * @implNote The tuple is implemented as a backwards linked list to allow for multiple longer tuples
 * to reuse the same existing left side. For example when joining multiple instances of a
 * class onto on existing tuple, every new tuple can reuse the existing tuple as it's {@link InstanceTuple#left}.
 */
public class InstanceTuple {

    private final @Nullable InstanceTuple left;
    private final @Nullable EObject right;

    public InstanceTuple(@Nullable InstanceTuple left, @Nullable EObject right) {
        this.left = left;
        this.right = right;
    }

    public InstanceTuple(EObject right) {
        this(null, right);
    }

    /**
     * @return contained instances from left to right
     */
    public Stream<@Nullable EObject> stream() {
        if (left == null) {
            return Stream.of(right);
        } else {
            return Stream.concat(left.stream(), Stream.<@Nullable EObject>of(right));
        }
    }

}
