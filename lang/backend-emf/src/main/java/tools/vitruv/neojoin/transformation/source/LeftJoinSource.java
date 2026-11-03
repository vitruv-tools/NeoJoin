package tools.vitruv.neojoin.transformation.source;

import static tools.vitruv.neojoin.transformation.source.SourceUtil.joinAndFilterByCondition;

import java.util.stream.Stream;

import tools.vitruv.neojoin.aqr.AQRJoin;
import tools.vitruv.neojoin.transformation.ExpressionEvaluator;
import tools.vitruv.neojoin.transformation.InstanceTuple;
import tools.vitruv.neojoin.transformation.TransformatorException;
import tools.vitruv.neojoin.utils.Result;
import tools.vitruv.neojoin.utils.Utils;

/**
 * Implements a left join between the given left and right instance sources.
 */
public class LeftJoinSource extends AbstractJoinSource {

    public LeftJoinSource(InstanceSource left, FromSource right, AQRJoin join, ExpressionEvaluator evaluator) {
        super(left, right, join, evaluator);
    }

    @Override
    public Stream<Result<InstanceTuple, TransformatorException>> get() {
        return leftSource.get()
            .flatMap(left -> {
                var joinAndFilter = joinAndFilterByCondition(rightSource, this::evaluateConditions);
                var results = joinAndFilter.apply(left);

                return Utils.defaultIfEmpty(results, () -> left.map(it -> new InstanceTuple(it, null)) );
            });
    }

}
