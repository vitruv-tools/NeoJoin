package tools.vitruv.neojoin.expression_parser.model;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * FindAny selects any element from a collection of elements. There are no guarantees which element
 * will be selected
 *
 * <p>Example expressions may look like
 *
 * <pre>
 *     {@code
 *          someResult = car.axis.findFirst()
 *          someResult = car.axis.findLast()
 *     }
 * </pre>
 *
 * Here, {@code X.findFirst()} and {@code X.findLast()} are FindAny operations
 */
public class FindAny implements ReferenceOperator {
    @Nullable ReferenceOperator followingOperator;

    public @Nullable ReferenceOperator getFollowingOperator() {
        return followingOperator;
    }

    public void setFollowingOperator(@Nullable ReferenceOperator followingOperator) {
        this.followingOperator = followingOperator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FindAny that)) {
            return false;
        }
        return Objects.equals(followingOperator, that.followingOperator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followingOperator);
    }

    @Override
    public String toString() {
        return "FindAny(followingOperator=" + followingOperator + ")";
    }
}
