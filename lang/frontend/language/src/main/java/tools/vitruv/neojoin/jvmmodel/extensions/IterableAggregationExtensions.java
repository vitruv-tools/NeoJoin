package tools.vitruv.neojoin.jvmmodel.extensions;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static tools.vitruv.neojoin.utils.Assertions.require;

class IterableAggregationExtensions {

	private IterableAggregationExtensions() {}

	public static <T> T single(Iterable<T> iterable) {
		var iterator = iterable.iterator();
		require(iterator.hasNext(), "Collection is empty");
		T single = iterator.next();
		require(!iterator.hasNext(), "Collection has more than one element");
		return single;
	}

	public static <T, M> @Nullable M same(Iterable<T> iterable, Function<T, M> map) {
		var iterator = iterable.iterator();
		if (!iterator.hasNext()) {
			return null;
		}
		M first = map.apply(iterator.next());
		while (iterator.hasNext()) {
			var current = map.apply(iterator.next());
			require(first.equals(current), "Collection has more than one different element");
		}
		return first;
	}

	public static <T> @Nullable T same(Iterable<T> iterable) {
		return same(iterable, v -> v);
	}

	public static <T> List<T> operator_plus(List<? extends T> first, List<? extends T> second) {
		return Stream.concat(first.stream(), second.stream()).toList();
	}

}
