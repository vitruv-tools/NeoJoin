package tools.vitruv.neojoin.transformation.source;

import tools.vitruv.neojoin.aqr.AQRJoin;
import tools.vitruv.neojoin.transformation.ExpressionEvaluator;
import tools.vitruv.neojoin.transformation.InstanceTuple;
import tools.vitruv.neojoin.utils.Utils;

import java.util.stream.Stream;

/**
 * Implements a left join between the given left and right instance sources.
 */
public class LeftJoinSource extends AbstractJoinSource {

    public LeftJoinSource(InstanceSource left, FromSource right, AQRJoin join, ExpressionEvaluator evaluator) {
        super(left, right, join, evaluator);
    }

    @Override
    public Stream<InstanceTuple> get() {
        return leftSource.get().flatMap(left -> {
            var results = rightSource.getEObjects()
                .filter(right -> evaluateConditions(left, right))
                .map(right -> new InstanceTuple(left, right));
            return Utils.defaultIfEmpty(results, () -> new InstanceTuple(left, null));
        });
    }

}
