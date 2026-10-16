package tools.vitruv.neojoin.utils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * A {@link Stream#collect(Collector)} that collects a stream of {@link Map.Entry} into a map.
 * Fail on duplicate keys.
 *
 * @param <K>              key type of the map +entries
 * @param <V>              value type of the map entries
 */
class ToMapFailOnDuplicates<K, V, E extends Exception> implements Collector<Pair<K, V>, ArrayList<Map<K, V>>, Result<Map<K, V>, E>> {

    private final TriFunction<K, V, V, E> exception;

    public ToMapFailOnDuplicates(TriFunction<K, V, V, E> exception) {
        this.exception = exception;
    }

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

            for (Map.Entry<K, V> e : m2.entrySet()) {
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
}
