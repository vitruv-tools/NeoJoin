package tools.vitruv.neojoin.transformation.source;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.neojoin.aqr.AQRJoin;
import tools.vitruv.neojoin.transformation.ExpressionEvaluator;
import tools.vitruv.neojoin.transformation.InstanceTuple;
import tools.vitruv.neojoin.utils.Utils;

import java.util.Objects;

/**
 * Abstract base class for join sources that provides functionality for evaluating join conditions.
 *
 * @see #evaluateConditions(InstanceTuple, EObject)
 */
public abstract class AbstractJoinSource implements InstanceSource {

    protected final InstanceSource leftSource;
    protected final FromSource rightSource;
    private final AQRJoin join;
    private final ExpressionEvaluator evaluator;

    public AbstractJoinSource(InstanceSource left, FromSource right, AQRJoin join, ExpressionEvaluator evaluator) {
        this.leftSource = left;
        this.rightSource = right;
        this.join = join;
        this.evaluator = evaluator;
    }

    protected boolean evaluateConditions(InstanceTuple left, EObject right) {
        return evaluateFeatureConditions(left, right) && evaluateExpressionConditions(left, right);
    }

    private boolean evaluateFeatureConditions(InstanceTuple left, EObject right) {
        return join.featureConditions().stream().allMatch(c -> evaluateFeatureCondition(c, left, right));
    }

    private boolean evaluateFeatureCondition(
        AQRJoin.FeatureCondition condition,
        InstanceTuple leftTuple,
        EObject right
    ) {
        var left = Utils.getAt(leftTuple.stream(), condition.otherIndex());
        //noinspection ConstantValue - false positive
        if (left == null) {
            return false;
        }

        for (var feature : condition.features()) {
            var leftValue = left.eGet(left.eClass().getEStructuralFeature(feature));
            var rightValue = right.eGet(right.eClass().getEStructuralFeature(feature));
            if (!Objects.equals(leftValue, rightValue)) {
                return false;
            }
        }

        return true;
    }

    private boolean evaluateExpressionConditions(InstanceTuple left, EObject right) {
        var context = evaluator.createContext(new InstanceTuple(left, right), join.from());
        return join.expressionConditions().stream().allMatch(context::evaluateCondition);
    }

}
