package tools.vitruv.neojoin.transformation.source;

import static tools.vitruv.neojoin.utils.Utils.allMatch;
import static tools.vitruv.neojoin.utils.Utils.collectFailingFast;
import static tools.vitruv.neojoin.transformation.ExceptionUtil.executeCatchingTransformatorException;

import java.util.Objects;

import org.eclipse.emf.ecore.EObject;

import tools.vitruv.neojoin.aqr.AQRJoin;
import tools.vitruv.neojoin.transformation.ExpressionEvaluator;
import tools.vitruv.neojoin.transformation.InstanceTuple;
import tools.vitruv.neojoin.transformation.TransformatorException;
import tools.vitruv.neojoin.utils.Utils;

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

    protected AbstractJoinSource(InstanceSource left, FromSource right, AQRJoin join, ExpressionEvaluator evaluator) {
        this.leftSource = left;
        this.rightSource = right;
        this.join = join;
        this.evaluator = evaluator;
    }

    protected boolean evaluateConditions(InstanceTuple left, EObject right) throws TransformatorException {
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

    private boolean evaluateExpressionConditions(InstanceTuple left, EObject right) throws TransformatorException {
        var context = evaluator.createContext(new InstanceTuple(left, right), join.from());
        return join.expressionConditions().stream()
            .map(expression -> executeCatchingTransformatorException(() -> context.evaluateCondition(expression)))
            .collect(collectFailingFast(allMatch(it -> it)))
            .valueUnsafe();
    }

}
