package tools.vitruv.neojoin.transformation.source;

import tools.vitruv.neojoin.aqr.AQRJoin;
import tools.vitruv.neojoin.transformation.ExpressionEvaluator;
import tools.vitruv.neojoin.transformation.InstanceTuple;

import java.util.stream.Stream;

/**
 * Implements an inner join between the given left and right instance sources.
 */
public class InnerJoinSource extends AbstractJoinSource {

    public InnerJoinSource(InstanceSource left, FromSource right, AQRJoin join, ExpressionEvaluator evaluator) {
        super(left, right, join, evaluator);
    }

    @Override
    public Stream<InstanceTuple> get() {
        return leftSource.get().flatMap(left ->
            rightSource.getEObjects()
                .filter(right -> evaluateConditions(left, right))
                .map(right -> new InstanceTuple(left, right))
        );
    }

}
