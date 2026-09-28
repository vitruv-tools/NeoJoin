package tools.vitruv.neojoin.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.stream.Collector.Characteristics;

/**
 * Various generic java utilities.
 */
public class Utils {

    private Utils() {}

    /**
     * {@link Stream#collect(Collector) Collects} a stream of {@link Map.Entry} into a map.
     *
     * @param <K>              key type of the map +entries
     * @param <V>              value type of the map entries
     * @return resulting map
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toMapIgnoreDuplicates() {
        return Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (a, b) -> a
        );
    }

    /**
     * {@link Stream#collect(Collector) Collects} a stream of {@link Map.Entry} into a map.
     * Fail on duplicate keys.
     *
     * @param <K>              key type of the map +entries
     * @param <V>              value type of the map entries
     * @return resulting map
     */
    public static <K, V, E extends Exception> Collector<Pair<K, V>, ?, Result<Map<K, V>, E>> toMapFailOnDuplicates(
            TriFunction<K, V, V, E> exception
    ) {
        return new Collector<Pair<K,V>, ArrayList<Map<K, V>>, Result<Map<K,V>, E>>() {

			@Override
			public Supplier<ArrayList<Map<K, V>>> supplier() {
                return () -> {
                    final ArrayList<Map<K, V>> resultOrEmpty = new ArrayList<>(1);
                    resultOrEmpty.add(new HashMap<>());
                    return resultOrEmpty;
                };
			}

			@Override
			public BiConsumer<ArrayList<Map<K, V>>, Pair<K, V>> accumulator() {
                return (result, next) -> {
                    if (result.size() == 1) {
                        final var previous = result.get(0).put(next.left(), next.right());
                        if (previous != null) result.add(Map.of(next.left(), previous));
                    }
                };
			}

			@Override
			public BinaryOperator<ArrayList<Map<K, V>>> combiner() {
				return (a, b) -> {
                    throw new UnsupportedOperationException("Unimplemented method 'combiner'");
                };
			}

			@Override
			public Function<ArrayList<Map<K, V>>, Result<Map<K, V>, E>> finisher() {
                return (it) -> {
                    if (it.size() == 1) {
                        return new Result.Success<>(it.get(0));
                    } else {
                        var key = it.get(1).keySet().stream().findAny().get();
                        var value1 = it.get(1).get(key);
                        var value2 = it.get(0).get(key);

				        return new Result.Failure<>(exception.apply(key, value1, value2));
                    }
                };
			}

			@Override
			public Set<Characteristics> characteristics() {
                return Set.of(Characteristics.UNORDERED);
			}

        };
    }

    public static <K, V, E extends Exception> Collector<Result<Pair<K, V>, E>, ?, Result<Map<K, List<V>>, E>> groupOrFail() {
        return Collector.<Result<Pair<K, V>, E>, OrFailSink<Map<K, List<V>>, Pair<K, V>, E>, Result<Map<K, List<V>>, E>>of(
                () -> new OrFailSink<>(
                                       new HashMap<>(),
                                       (map, pair) -> map
                                       .computeIfAbsent(pair.left(), (key) -> new ArrayList<>())
                                       .add(pair.right())),
                OrFailSink::insert,
                (a, b) -> { throw new UnsupportedOperationException("Parallel execution of stream is unsupported."); },
                OrFailSink::get,
                Characteristics.UNORDERED
                );
    }

    /* NOTE: Since the intermediate accumulation type of the [Collector] is often hidden
     *      as an implementation detail, its is unfortunately necessary to omit for the
     *      type of the collector argument and thus it is necessary to conduct unchecked type casts.
     */
	@SuppressWarnings("unchecked")
	public static <T, E extends Exception, D> Result<D, E> collectOrFailOnFirstFailure(
            Stream<Result<T, E>> stream,
            Collector<T, ?, D> collector
    ) {
        final var sink = collector.supplier().get();
        final var iter = stream.iterator();
        final var accumulator = (BiConsumer<Object, T>) collector.accumulator();
        final var finisher = (Function<Object, D>) collector.finisher();
        while (iter.hasNext()) {
            final var next = iter.next();
            if (next instanceof Result.Success<T, ?> success) accumulator.accept(sink, success.value());
            else if (next instanceof Result.Failure<?, E> failure) return Result.fail(failure.throwable());
            else throw new IllegalStateException("Unknown type of sealed interface.");
        }

        return Result.of(finisher.apply(sink));
    }

    private static final class OrFailSink<S, V, E extends Throwable> {
        private Result<S, E> result;
        private BiConsumer<S, V> insert;

        public OrFailSink(S sink, BiConsumer<S, V> insert) {
            this.result = Result.of(sink);
            this.insert = insert;
        }

        public void insert(Result<V, E> element) {
            if (result instanceof Result.Success<S, E> sink) {
                if (element instanceof Result.Success<V, E> success) {
                    insert.accept(sink.value(), success.value());
                } else if (element instanceof Result.Failure<V, E> failure) {
                    result = Result.fail(failure.throwable());
                }
            }
        }

        public Result<S, E> get() {
            return result;
        }
    }

    /**
     * This function converts a stream into an [Iterable].
     * This allows to iterate over the stream using a normal for loop.
     *
     * @param stream the stream that should be turned into an iterator
     * @return an iterator for the elements of the stream.
     */
    public static <T> Iterable<T> iter(Stream<T> stream) {
        return () -> stream.iterator();
    }

    public static String removeSuffix(String string, String suffix) {
        if (string.endsWith(suffix)) {
            return string.substring(0, string.length() - suffix.length());
        } else {
            return string;
        }
    }

    public static <T> Stream<T> streamOf(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }

    public static <T> int indexOf(Stream<T> stream, T element) {
        return stream.toList().indexOf(element);
    }

    /**
     * Returns the element at the given index from the given stream.
     *
     * @param stream the stream to get the element from
     * @param index  the index of the element to get
     * @param <T>    the type of stream elements
     * @return the element at the given index
     * @throws IndexOutOfBoundsException if the index is out of bounds
     * @implNote This method cannot return {@link Optional} because the stream may contain {@code null} values.
     */
    public static <T> T getAt(Stream<T> stream, int index) {
        var it = stream.skip(index).iterator();
        if (!it.hasNext()) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        return it.next();
    }

    public static <T> void forEachIndexed(Iterator<T> it, BiConsumer<T, Integer> consumer) {
        var index = 0;
        while (it.hasNext()) {
            consumer.accept(it.next(), index);
            index++;
        }
    }

    public static <T> void forEachIndexed(Iterable<T> iterable, BiConsumer<T, Integer> consumer) {
        forEachIndexed(iterable.iterator(), consumer);
    }

    /**
     * Returns a stream which contains all elements from the given stream if there are any. If the given stream is empty,
     * returns a stream with a single value retrieved from the given default value supplier.
     *
     * @param stream       input stream of elements
     * @param defaultValue supplier for a default value if the input is empty
     * @param <T>          type of the values
     * @return output stream
     */
    public static <T> Stream<T> defaultIfEmpty(Stream<T> stream, Supplier<T> defaultValue) {
        var iterator = stream.iterator();
        if (iterator.hasNext()) {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
        } else {
            return Stream.of(defaultValue.get());
        }
    }

    /**
     * Returns a stream of elements from the given list in reverse order.
     *
     * @param list the list to reverse
     * @param <E>  type of the elements in the list
     * @return stream of elements in reverse order
     */
    public static <E> Stream<E> reversedStream(List<E> list) {
        var it = new Iterator<E>() {
            int currentIndex = list.size();

            @Override
            public boolean hasNext() {
                return currentIndex > 0;
            }

            @Override
            public E next() {
                return list.get(--currentIndex);
            }
        };

        return StreamSupport.stream(
            Spliterators.spliterator(it, list.size(), 0),
            false
        );
    }

    /**
     * Returns a stream of pairs containing each element from the given input stream and its index in the stream.
     *
     * @param stream the stream to index
     * @return stream of pairs containing each element and its index
     */
    public static <T> Stream<Pair<T, Integer>> indexed(Stream<T> stream) {
        var index = new Mutable<>(0);
        //noinspection DataFlowIssue - false positive
        return stream.map(e -> new Pair<>(e, index.value++));
    }

}
