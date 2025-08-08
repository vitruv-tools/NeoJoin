package tools.vitruv.neojoin.utils;

/**
 * Represents the result of an operation that can either {@link Success succeed} by returning a value or {@link Failure fail}
 * with an exception.
 *
 * @param <T> type of the return value if successful
 */
public sealed interface Result<T> {

    record Success<T>(
        T value
    ) implements Result<T> {}

    record Failure<T>(
        Throwable throwable
    ) implements Result<T> {}

}
