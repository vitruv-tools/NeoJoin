package tools.vitruv.optggs.transpiler.operators.reference_operators;

import java.util.Objects;
import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRulesBuilder;

import java.util.List;

public class ResolvedReferenceOperatorChain {
    private final List<ResolvedReferenceOperator> referenceOperators;
    private final FQN targetRoot;

    public ResolvedReferenceOperatorChain(List<ResolvedReferenceOperator> referenceOperators, FQN targetRoot) {
        this.referenceOperators = referenceOperators;
        this.targetRoot = targetRoot;
    }

    public List<ResolvedReferenceOperator> getReferenceOperators() {
        return referenceOperators;
    }

    public FQN getTargetRoot() {
        return targetRoot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResolvedReferenceOperatorChain other)) return false;
        return Objects.equals(referenceOperators, other.referenceOperators) && Objects.equals(targetRoot, other.targetRoot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceOperators, targetRoot);
    }

    @Override
    public String toString() {
        return "ResolvedReferenceOperatorChain(" + "referenceOperators=" + referenceOperators + ", " + "targetRoot=" + targetRoot + ")";
    }

    public TripleRulesBuilder generateRulesAndConstraints() {
        TripleRulesBuilder builder = new TripleRulesBuilder();

        final TripleRule featureCallRule;
        if (referenceOperators.getFirst() instanceof ResolvedFeatureCall featureCall) {
            featureCallRule = featureCall.createFeatureCallRule(targetRoot, builder);
        } else {
            throw new IllegalStateException("First Reference Operator must be a FeatureCall");
        }

        final List<ResolvedReferenceOperator> remainingOperators =
                referenceOperators.subList(1, referenceOperators.size());
        remainingOperators.forEach(
                resolvedReferenceOperator -> resolvedReferenceOperator.extendRules(builder));

        // If the feature call rule wasn't extended with any green elements, we need to remove it to
        // avoid conflicting rules
        if (!featureCallRule.hasAnyGreenElements()) {
            builder.removeRule(featureCallRule);
        }

        return builder;
    }
}
