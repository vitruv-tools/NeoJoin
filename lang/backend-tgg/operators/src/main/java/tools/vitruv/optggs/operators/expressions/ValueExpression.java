package tools.vitruv.optggs.operators.expressions;

/** An expression that can bei either a Constant or Variable */
public interface ValueExpression {
    ValueExpression deepCopy();
}
