package tools.vitruv.neojoin.transformation.source;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.XExpression;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.transformation.ExpressionEvaluator;
import tools.vitruv.neojoin.transformation.InstanceTuple;
import tools.vitruv.neojoin.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static tools.vitruv.neojoin.utils.Assertions.check;

/**
 * Mimics the {@link InstanceSource instance source interface} for a {@code group by} clause. However, this class does
 * not implement the interface because it cannot conform to it, because an {@link InstanceTuple} can only hold {@link EObject}s
 * and the result of a {@code group by} is not a tuple of objects but instead a tuple of lists of objects.
 */
public class GroupingSource {

    private final List<XExpression> groupingExpressions;
    private final InstanceSource inner;
    private final ExpressionEvaluator evaluator;

    public GroupingSource(List<XExpression> groupingExpressions, InstanceSource inner, ExpressionEvaluator evaluator) {
        this.groupingExpressions = groupingExpressions;
        this.inner = inner;
        this.evaluator = evaluator;
    }

    public Stream<List<List<EObject>>> get() {
        var grouped = inner.get().collect(Collectors.groupingBy(this::getGroupingKey));
        return grouped.values().stream().map(GroupingSource::map);
    }

    private List<?> getGroupingKey(InstanceTuple tuple) {
        var context = evaluator.createContext(tuple, null);
        return groupingExpressions.stream().map(context::evaluateExpression).toList();
    }

    /**
     * Transforms a list of tuples of objects into a tuple of lists of objects where the latter tuple is
     * represented by another list. In SQL terms, this function transforms a list of rows into a list of columns.
     */
    private static List<List<@Nullable EObject>> map(List<InstanceTuple> tuples) {
        check(!tuples.isEmpty());
        var tupleLength = tuples.getFirst().stream().count();
        List<List<@Nullable EObject>> result = createResultList(tupleLength, tuples.size());

        for (var tuple : tuples) {
            Utils.forEachIndexed(
                tuple.stream().iterator(), (obj, index) -> {
                    result.get(index).add(obj);
                }
            );
        }

        return result;
    }

    private static List<List<@Nullable EObject>> createResultList(long tupleLength, int numTuples) {
        return LongStream.range(0, tupleLength)
            .mapToObj(i -> (List<@Nullable EObject>) new ArrayList<@Nullable EObject>(numTuples))
            .toList();
    }

}
