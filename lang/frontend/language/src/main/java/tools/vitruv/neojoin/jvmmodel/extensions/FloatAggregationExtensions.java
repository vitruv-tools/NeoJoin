package tools.vitruv.neojoin.jvmmodel.extensions;

import java.util.function.Function;

class FloatAggregationExtensions {

	private FloatAggregationExtensions() {}

	public static <T> float sum(Iterable<T> iterable, Function<T, Float> map) {
		float sum = 0;
		for (var value : iterable) {
			sum += map.apply(value);
		}
		return sum;
	}

	public static float sum(Iterable<Float> iterable) {
		return sum(iterable, v -> v);
	}

	public static <T> float min(Iterable<T> iterable, Function<T, Float> map) {
		float min = Float.MAX_VALUE;
		for (var v : iterable) {
			var value = map.apply(v);
			if (value < min) {
				min = value;
			}
		}
		return min;
	}

	public static <T> float max(Iterable<T> iterable, Function<T, Float> map) {
		float max = Float.MIN_VALUE;
		for (var v : iterable) {
			var value = map.apply(v);
			if (value > max) {
				max = value;
			}
		}
		return max;
	}

	public static <T> double avg(Iterable<T> iterable, Function<T, Float> map) {
		return DoubleAggregationExtensions.avg(iterable, v -> map.apply(v).doubleValue());
	}

	public static double avg(Iterable<Float> iterable) {
		return avg(iterable, Function.identity());
	}

}
