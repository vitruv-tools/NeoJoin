package tools.vitruv.neojoin.jvmmodel.extensions;

import java.util.function.Function;

class ShortAggregationExtensions {

	private ShortAggregationExtensions() {}

	public static <T> short sum(Iterable<T> iterable, Function<T, Short> map) {
		short sum = 0;
		for (var value : iterable) {
			sum += map.apply(value);
		}
		return sum;
	}

	public static short sum(Iterable<Short> iterable) {
		return sum(iterable, v -> v);
	}

	public static <T> short min(Iterable<T> iterable, Function<T, Short> map) {
		short min = Short.MAX_VALUE;
		for (var v : iterable) {
			var value = map.apply(v);
			if (value < min) {
				min = value;
			}
		}
		return min;
	}

	public static <T> short max(Iterable<T> iterable, Function<T, Short> map) {
		short max = Short.MIN_VALUE;
		for (var v : iterable) {
			var value = map.apply(v);
			if (value > max) {
				max = value;
			}
		}
		return max;
	}

	public static <T> double avg(Iterable<T> iterable, Function<T, Short> map) {
		return DoubleAggregationExtensions.avg(iterable, v -> map.apply(v).doubleValue());
	}

	public static double avg(Iterable<Short> iterable) {
		return avg(iterable, Function.identity());
	}

}
