package tools.vitruv.neojoin.utils;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * Represents the result of an operation that can either {@link Success succeed} by returning a value or {@link Failure fail}
 * with an exception.
 *
 * @param <T> type of the return value if successful
 */
@SuppressWarnings("unused") // suppress unused generic T since it is needed
public sealed interface Result<T extends @Nullable Object, E extends Throwable> {

    <V> Result<V, E> map(Function<T, V> function);
    <V> Result<V, E> bind(Function<T, Result<V, E>> function);
    T valueUnsafe() throws E;

    record Success<T, E extends Throwable>(
        T value
    ) implements Result<T, E> {

        public <F extends Throwable> Success<T, F> cast() {
            return new Success<>(value);
        }

        @Override
        public <V> Result<V, E> map(Function<T, V> function) {
            return new Success<>(function.apply(value));
        }

        @Override
        public <V> Result<V, E> bind(Function<T, Result<V, E>> function) {
            return function.apply(value);
        }

        @Override
        public T valueUnsafe() {
            return value;
        }

    }

    record Failure<T, E extends Throwable>(
        E throwable
    ) implements Result<T, E> {

        public <V> Failure<V, E> cast() {
            return new Failure<>(throwable);
        }

        @Override
        public <V> Result<V, E> map(Function<T, V> function) {
            return cast();
        }

        @Override
        public <V> Result<V, E> bind(Function<T, Result<V, E>> function) {
            return cast();
        }

        @Override
        public T valueUnsafe() throws E {
            throw throwable;
        }

    }

    static <T, E extends Throwable> Result<T, E> of(T value) {
        return new Success<T, E>(value);
    }

    static <T, E extends Throwable> Result<T, E> fail(E exception) {
        return new Failure<T, E>(exception);
    }

    static <T> Result<T, Exception> ofCaught(Supplier<T> supplier) {
        try {
            return Result.of(supplier.get());
        } catch (Exception e) {
            return Result.fail(e);
        }
    }

}
