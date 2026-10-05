package tools.vitruv.neojoin.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
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

    public static <T> Collector<T, ?, Boolean> allMatch(Predicate<T> predicate) {
        return Collectors.mapping(it -> predicate.test(it), Collectors.reducing(true, (a, b) -> a && b));
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
                return (container, next) -> {
                    if (!isFailure(container)) {
                        final var previous = container.get(0).put(next.left(), next.right());
                        if (previous != null) fail(container, next.left(), previous);
                    }
                };
			}

			@Override
			public BinaryOperator<ArrayList<Map<K, V>>> combiner() {
				return (a, b) -> {
                    if (isFailure(a)) return a;
                    else if (isFailure(b)) return b;

                    final var m1 = a.get(0);
                    final var m2 = b.get(0);

                    for (Map.Entry<K,V> e : m2.entrySet()) {
                        K k = e.getKey();
                        V v = Objects.requireNonNull(e.getValue());
                        V u = m1.putIfAbsent(k, v);

                        if (u != null) {
                            fail(a, k, v);
                            break;
                        }
                    }

                    return a;
                };
			}

			@Override
			public Function<ArrayList<Map<K, V>>, Result<Map<K, V>, E>> finisher() {
                return (it) -> {
                    if (isFailure(it)) {
				        return new Result.Failure<>(getFailure(it, exception));
                    } else {
                        return new Result.Success<>(it.get(0));
                    }
                };
			}

			@Override
			public Set<Characteristics> characteristics() {
                return Set.of(Characteristics.UNORDERED, Characteristics.CONCURRENT);
			}

            private static <K, V, E extends Exception> boolean isFailure(ArrayList<Map<K, V>> container) {
                return container.size() != 1;
            }

            private static <K, V, E extends Exception> void fail(ArrayList<Map<K, V>> container, K key, V duplicateValue) {
                container.add(Map.of(key, duplicateValue));
            }

            private static <K, V, E extends Exception> E getFailure(ArrayList<Map<K, V>> container, TriFunction<K, V, V, E> exception) {
                if (isFailure(container)) {
                    var key = container.get(1).keySet().stream().findAny().get();
                    var value1 = container.get(1).get(key);
                    var value2 = container.get(0).get(key);
                    return exception.apply(key, value1, value2);
                }
                return null;
            }
        };
    }

    public static <K, V, E extends Exception> Collector<Result<Pair<K, V>, E>, ?, Result<Map<K, List<V>>, E>> groupOrFail() {
        return collectFailingFast(
                Collectors.groupingBy(Pair::left, HashMap::new,
                    Collectors.mapping(Pair::right, Collectors.toList()))
                );
    }

   /* NOTE: Since the intermediate accumulation type of the [Collector] is often hidden
    *      as an implementation detail, it's unfortunately necessary to omit for the
    *      type of the collector argument and thus it is necessary to conduct unchecked type casts.
    */
	@SuppressWarnings("unchecked")
    public static <T, D, E extends Exception> Collector<Result<T, E>, ?, Result<D, E>> collectFailingFast(
            Collector<T, ?, D> collector
    ) {
        final var supplier = collector.supplier();
        final var accumulator = (BiConsumer<Object, T>) collector.accumulator();
        final var combiner = (BinaryOperator<Object>) collector.combiner();
        final var finisher = (Function<Object, D>) collector.finisher();
        final var characteristics = collector.characteristics().stream()
            .filter(it -> it != Characteristics.IDENTITY_FINISH)
            .toArray(Characteristics[]::new);

        return Collector.of(
            () -> {
                final ArrayList<Result<Object, E>> container = new ArrayList<>(1);
                container.add(Result.of(supplier.get()));
                return container;
            },
            (container, next) ->
                container.get(0).ifSuccess(sink ->
                    next
                        .ifSuccess(value -> accumulator.accept(sink, value))
                        .ifFailure(e -> container.set(0, Result.fail(e)))
                ),
            (a, b) -> {
                a.set(0, a.get(0).bind(valueOfA ->
                        b.get(0).map(valueOfB -> combiner.apply(valueOfA, valueOfB))));
                return a;
            },
            container -> container.get(0).map(finisher),
            characteristics
        );
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
