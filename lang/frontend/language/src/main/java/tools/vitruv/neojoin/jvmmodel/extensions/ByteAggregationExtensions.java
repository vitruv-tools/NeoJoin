package tools.vitruv.neojoin.jvmmodel.extensions;

import java.util.function.Function;

class ByteAggregationExtensions {

    private ByteAggregationExtensions() {}

    public static <T> byte sum(Iterable<T> iterable, Function<T, Byte> map) {
        byte sum = 0;
        for (var value : iterable) {
            sum += map.apply(value);
        }
        return sum;
    }

    public static byte sum(Iterable<Byte> iterable) {
        return sum(iterable, v -> v);
    }

    public static <T> byte min(Iterable<T> iterable, Function<T, Byte> map) {
        byte min = Byte.MAX_VALUE;
        for (var v : iterable) {
            var value = map.apply(v);
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    public static <T> byte max(Iterable<T> iterable, Function<T, Byte> map) {
        byte max = Byte.MIN_VALUE;
        for (var v : iterable) {
            var value = map.apply(v);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    public static <T> double avg(Iterable<T> iterable, Function<T, Byte> map) {
        return DoubleAggregationExtensions.avg(iterable, v -> map.apply(v).doubleValue());
    }

    public static double avg(Iterable<Byte> iterable) {
        return avg(iterable, Function.identity());
    }

}
