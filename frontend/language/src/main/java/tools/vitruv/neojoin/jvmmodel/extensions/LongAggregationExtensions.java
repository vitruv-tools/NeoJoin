package tools.vitruv.neojoin.jvmmodel.extensions;

import java.util.function.Function;

class LongAggregationExtensions {

    private LongAggregationExtensions() {}

    public static <T> long sum(Iterable<T> iterable, Function<T, Long> map) {
        long sum = 0;
        for (var value : iterable) {
            sum += map.apply(value);
        }
        return sum;
    }

    public static long sum(Iterable<Long> iterable) {
        return sum(iterable, v -> v);
    }

    public static <T> long min(Iterable<T> iterable, Function<T, Long> map) {
        long min = Long.MAX_VALUE;
        for (var v : iterable) {
            var value = map.apply(v);
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    public static <T> long max(Iterable<T> iterable, Function<T, Long> map) {
        long max = Long.MIN_VALUE;
        for (var v : iterable) {
            var value = map.apply(v);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    public static <T> double avg(Iterable<T> iterable, Function<T, Long> map) {
        return DoubleAggregationExtensions.avg(iterable, v -> map.apply(v).doubleValue());
    }

    public static double avg(Iterable<Long> iterable) {
        return avg(iterable, Function.identity());
    }

}
