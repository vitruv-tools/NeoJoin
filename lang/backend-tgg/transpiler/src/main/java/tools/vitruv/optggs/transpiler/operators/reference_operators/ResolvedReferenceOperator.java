package tools.vitruv.optggs.transpiler.operators.reference_operators;

import tools.vitruv.optggs.transpiler.graph.tgg.TripleRulesBuilder;

public interface ResolvedReferenceOperator {
    void extendRules(TripleRulesBuilder builder);
}
