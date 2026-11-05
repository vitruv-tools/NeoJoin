package tools.vitruv.neojoin.aqr;

import org.eclipse.xtext.xbase.XExpression;

import java.util.List;

/**
 * Represents a join of the given type with the given source class.
 *
 * @param type                 join type
 * @param from                 class to join with
 * @param featureConditions    common features to join on
 * @param expressionConditions Xtend expression to join on
 */
public record AQRJoin(
    Type type,
    AQRFrom from,
    List<FeatureCondition> featureConditions,
    List<XExpression> expressionConditions
) {

    public enum Type {
        Inner,
        Left,
    }

    /**
     * A join condition based on the given common features between the class of the current join
     * and another class as specified by {@link #otherIndex}.
     *
     * @param otherIndex the index of the other source class that this join condition applies to in the order defined by {@link AQRSource#allFroms()}
     * @param features   the features to join on
     */
    public record FeatureCondition(
        int otherIndex,
        List<String> features
    ) {}

}
