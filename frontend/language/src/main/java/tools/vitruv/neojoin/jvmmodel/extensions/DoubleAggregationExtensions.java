package tools.vitruv.neojoin.jvmmodel.extensions;

import java.util.function.Function;

class DoubleAggregationExtensions {

    private DoubleAggregationExtensions() {}

    public static <T> double sum(Iterable<T> iterable, Function<T, Double> map) {
        double sum = 0;
        for (var value : iterable) {
            sum += map.apply(value);
        }
        return sum;
    }

    public static double sum(Iterable<Double> iterable) {
        return sum(iterable, v -> v);
    }

    public static <T> double min(Iterable<T> iterable, Function<T, Double> map) {
        double min = Double.MAX_VALUE;
        for (var v : iterable) {
            var value = map.apply(v);
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    public static <T> double max(Iterable<T> iterable, Function<T, Double> map) {
        double max = Double.MIN_VALUE;
        for (var v : iterable) {
            var value = map.apply(v);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    public static <T> double avg(Iterable<T> iterable, Function<T, Double> map) {
        double sum = 0;
        int count = 0;
        for (var value : iterable) {
            sum += map.apply(value);
            count++;
        }
        if (count == 0) {
            return 0;
        } else {
            return sum / count;
        }
    }

    public static double avg(Iterable<Double> iterable) {
        return avg(iterable, Function.identity());
    }

}
