package tools.vitruv.optggs.transpiler.operators;

import tools.vitruv.optggs.transpiler.tgg.TripleRule;

public interface RuleGenerator {
    void extendRule(TripleRule rule);
}
