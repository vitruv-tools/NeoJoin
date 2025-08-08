package tools.vitruv.neojoin.jvmmodel.extensions;

import java.util.Collection;
import java.util.List;

public final class Extensions {

    public static final Collection<Class<?>> All = List.of(
        IterableAggregationExtensions.class,
        DoubleAggregationExtensions.class,
        FloatAggregationExtensions.class,
        LongAggregationExtensions.class,
        IntegerAggregationExtensions.class,
        ShortAggregationExtensions.class,
        ByteAggregationExtensions.class
    );

    private Extensions() {}

}
