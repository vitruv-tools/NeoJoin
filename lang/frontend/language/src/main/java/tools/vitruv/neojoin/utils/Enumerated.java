package tools.vitruv.neojoin.utils;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

import tools.vitruv.neojoin.utils.Enumerated.IndexedValue;

public final class Enumerated<T> implements Iterable<IndexedValue<T>> {

    final Iterable<T> elements;

    public Enumerated(final Iterable<T> elements) {
        this.elements = elements;
    }

    @Override
    public Iterator<IndexedValue<T>> iterator() {
        return new Iterator<>() {
            final IntVar counter = IntVar.of(0);
            final Iterator<T> iter = elements.iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedValue<T> next() {
                return IndexedValue.of(counter.increaseAndGet(), iter.next());
            }
        };
    }

	public static <T> Enumerated<T> enumerate(final Iterable<T> elements) {
		return new Enumerated<>(elements);
	}

	public static <T> Enumerated<T> enumerate(final Iterator<T> elements) {
		return enumerate(() -> elements);
	}

    public static <T> Enumerated<T> enumerate(final Stream<T> elements) {
        return enumerate(elements::iterator);
    }

    /**
     * Creates closure adding an index to elements.
     * It can e.g. be used to add an index for elements of a stream such as:
     * <pre> stream.map(enumerated()) </pre>
     *
     * NOTE: This implementation is not threadsafe.
     *
     * @return closure adding an index to elements.
     * @param <T> type of elements
     */
    public static <T> Function<T, IndexedValue<T>> enumerated() {
        final IntVar index = IntVar.of(0);
        return it -> IndexedValue.of(index.increaseAndGet(), it);
    }

	public record IndexedValue<T> (int index, T value) {

		public static <T> IndexedValue<T> of(int index, T value) {
			return new IndexedValue<>(index, value);
		}
	}
}
