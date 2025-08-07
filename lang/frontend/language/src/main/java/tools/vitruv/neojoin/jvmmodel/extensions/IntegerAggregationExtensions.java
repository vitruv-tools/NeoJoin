package tools.vitruv.neojoin.jvmmodel.extensions;

import java.util.function.Function;

class IntegerAggregationExtensions {

	private IntegerAggregationExtensions() {}

	public static <T> int sum(Iterable<T> iterable, Function<T, Integer> map) {
		int sum = 0;
		for (var value : iterable) {
			sum += map.apply(value);
		}
		return sum;
	}

	public static int sum(Iterable<Integer> iterable) {
		return sum(iterable, v -> v);
	}

	public static <T> int min(Iterable<T> iterable, Function<T, Integer> map) {
		int min = Integer.MAX_VALUE;
		for (var v : iterable) {
			var value = map.apply(v);
			if (value < min) {
				min = value;
			}
		}
		return min;
	}

	public static <T> int max(Iterable<T> iterable, Function<T, Integer> map) {
		int max = Integer.MIN_VALUE;
		for (var v : iterable) {
			var value = map.apply(v);
			if (value > max) {
				max = value;
			}
		}
		return max;
	}

	public static <T> double avg(Iterable<T> iterable, Function<T, Integer> map) {
		return DoubleAggregationExtensions.avg(iterable, v -> map.apply(v).doubleValue());
	}

	public static double avg(Iterable<Integer> iterable) {
		return avg(iterable, Function.identity());
	}

}
