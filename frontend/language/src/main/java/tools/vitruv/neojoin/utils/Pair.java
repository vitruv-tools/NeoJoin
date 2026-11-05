package tools.vitruv.neojoin.utils;

import org.jspecify.annotations.Nullable;

/**
 * An immutable pair of two values.
 *
 * @param left  left or first value
 * @param right right or second value
 * @param <L>   type of left value
 * @param <R>   type of right value
 */
public record Pair<L extends @Nullable Object, R extends @Nullable Object>(
    L left,
    R right
) {}
