package tools.vitruv.neojoin.transformation.source;

import static tools.vitruv.neojoin.transformation.source.SourceUtil.joinAndFilterByCondition;

import java.util.stream.Stream;

import tools.vitruv.neojoin.aqr.AQRJoin;
import tools.vitruv.neojoin.transformation.ExpressionEvaluator;
import tools.vitruv.neojoin.transformation.InstanceTuple;
import tools.vitruv.neojoin.transformation.TransformatorException;
import tools.vitruv.neojoin.utils.Result;

/**
 * Implements an inner join between the given left and right instance sources.
 */
public class InnerJoinSource extends AbstractJoinSource {

    public InnerJoinSource(InstanceSource left, FromSource right, AQRJoin join, ExpressionEvaluator evaluator) {
        super(left, right, join, evaluator);
    }

    @Override
    public Stream<Result<InstanceTuple, TransformatorException>> get() {
        return leftSource.get()
            .flatMap(joinAndFilterByCondition(rightSource, this::evaluateConditions));
    }

}
