package tools.vitruv.neojoin.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Various generic java utilities.
 */
public class Utils {

    /**
     * {@link Stream#collect(Collector) Collects} a stream of {@link Map.Entry} into a map.
     *
     * @param throwOnDuplicate how to handle duplicates
     * @param <K>              key type of the map +entries
     * @param <V>              value type of the map entries
     * @return resulting map
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> mapCollector(boolean throwOnDuplicate) {
        return Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            throwOnDuplicate ? (a, b) -> {
                throw new IllegalStateException("duplicate key");
            } : (a, b) -> a
        );
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
