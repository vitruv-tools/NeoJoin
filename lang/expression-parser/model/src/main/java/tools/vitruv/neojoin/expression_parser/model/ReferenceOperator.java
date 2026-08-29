package tools.vitruv.neojoin.expression_parser.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A ReferenceOperator is a (partial) parsed expression that can be used for model transformations.
 * It contains the required properties (e.g. type, field names) for the following steps and possibly
 * the following ReferenceOperator-chain
 */
public interface ReferenceOperator {
    @Nullable ReferenceOperator getFollowingOperator();

    void setFollowingOperator(ReferenceOperator followingOperator);

    /** Returns the last ReferenceOperator in this chain */
    @NonNull
    default ReferenceOperator getLastOperatorInChain() {
        ReferenceOperator lastOperator = this;
        while (lastOperator.getFollowingOperator() != null) {
            lastOperator = lastOperator.getFollowingOperator();
        }
        return lastOperator;
    }
}
