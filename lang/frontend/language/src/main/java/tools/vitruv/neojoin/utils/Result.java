package tools.vitruv.neojoin.utils;

import java.util.function.Consumer;
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

    public static <T, E extends Exception> Result<T, E>  executeCatching(
            Class<E> exceptionType,
            ThrowingSupplier<T, E> f)
    {
        try {
            return Result.of(f.supply());
        } catch (Exception e) {
            if (exceptionType.isInstance(e)) {
                @SuppressWarnings("unchecked")
                var castedException = (E) e;
                return Result.fail(castedException);
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                /* Due to checked exceptions this case can not occur.
                 * This is because the only checked exceptions that can be thrown by
                 * f have to be of type E. */
                throw new RuntimeException(e);
            }
        }
    }

    <V> Result<V, E> map(Function<T, V> function);

    <V> Result<V, E> bind(Function<T, Result<V, E>> function);
    T valueUnsafe() throws E;

    <V> V fold(Function<T, V> success, Function<E, V> error);

    Result<T, E> ifSuccess(Consumer<T> function);

    Result<T, E> ifFailure(Consumer<E> function);

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

		@Override
		public Result<T, E> ifSuccess(Consumer<T> function) {
            function.accept(value);
            return this;
		}

		@Override
		public Result<T, E> ifFailure(Consumer<E> function) {
            // Do nothing since this is not a failure.
            return this;
		}

		@Override
		public <V> V fold(Function<T, V> success, Function<E, V> error) {
            return success.apply(value());
		}

		@Override
        public boolean equals(Object other) {
            return other instanceof Result.Success success && success.value().equals(this.value());
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

		@Override
		public Result<T, E> ifSuccess(Consumer<T> function) {
            // Do nothing since this is not a success.
            return this;
		}

		@Override
		public Result<T, E> ifFailure(Consumer<E> function) {
            function.accept(throwable);
            return this;
		}

		@Override
		public <V> V fold(Function<T, V> success, Function<E, V> error) {
            return error.apply(throwable());
		}

		@Override
        public boolean equals(Object other) {
            return other instanceof Result.Failure failure && failure.throwable().equals(this.throwable());
        }
    }

    static <T, E extends Throwable> Result<T, E> of(T value) {
        return new Success<T, E>(value);
    }

    static <T, E extends Throwable> Result<T, E> fail(E exception) {
        return new Failure<T, E>(exception);
    }
}
