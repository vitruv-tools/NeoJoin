package tools.vitruv.optggs.operators.reference_operator;

import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.optggs.operators.FQN;

public record ReferenceOperatorChain(
        String sourceNamespace,
        FQN targetRoot,
        FQN targetLeaf,
        String targetField,
        ReferenceOperator referenceOperator) {
    @Override
    public String toString() {
        return "π(" + referenceOperator.toString() + ")";
    }
}
