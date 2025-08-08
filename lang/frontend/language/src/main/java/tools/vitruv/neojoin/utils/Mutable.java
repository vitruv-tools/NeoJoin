package tools.vitruv.neojoin.utils;

import org.jspecify.annotations.Nullable;

/**
 * Represents a mutable variable that can be mutated from within a lambda.
 *
 * @param <T> type of the variable
 */
public final class Mutable<T extends @Nullable Object> {

    public T value;

    public Mutable(T initialValue) {
        this.value = initialValue;
    }

}
