package tools.vitruv.neojoin.aqr;

import org.eclipse.xtext.xbase.XExpression;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Source of a class.
 *
 * @param from                the main source (e.g. {@code from Class c})
 * @param joins               joins
 * @param condition           Xtend expression conditions (e.g. {@code where a + 5 == b})
 * @param groupingExpressions Xtend expressions to group by (e.g. {@code group by a.name, b.id})
 */
public record AQRSource(
    AQRFrom from,
    List<AQRJoin> joins,
    @Nullable XExpression condition,
    List<XExpression> groupingExpressions
) {

    /**
     * Get a stream of all contained {@link AQRFrom}.
     *
     * @implNote {@link AQRJoin.FeatureCondition join feature conditions} depend on the order returned by this method
     */
    public Stream<AQRFrom> allFroms() {
        return Stream.concat(
            Stream.of(from),
            joins.stream().map(AQRJoin::from)
        );
    }

}
