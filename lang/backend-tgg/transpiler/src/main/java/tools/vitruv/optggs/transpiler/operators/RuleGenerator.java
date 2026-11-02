package tools.vitruv.optggs.transpiler.operators;

import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;

public interface RuleGenerator {
    void extendRule(TripleRule rule);
}
