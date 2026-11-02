package tools.vitruv.optggs.transpiler.graph.tgg;

import tools.vitruv.optggs.transpiler.graph.NameRepositoryFixtures;

import java.util.ArrayList;

public class TripleRuleFixtures {
    private static TripleRule.TripleRuleBuilder someTripleRuleBuilder() {
        return TripleRule.builder()
                .nameRepository(NameRepositoryFixtures.someNameRepository())
                .sourceNodes(new ArrayList<>())
                .targetNodes(new ArrayList<>())
                .correspondences(new ArrayList<>())
                .constraints(new ArrayList<>());
    }

    public static TripleRule someTripleRule(
            ArrayList<TGGNode> sourceNodes,
            ArrayList<TGGNode> targetNodes,
            ArrayList<Correspondence> correspondences) {
        return someTripleRuleBuilder()
                .sourceNodes(sourceNodes)
                .targetNodes(targetNodes)
                .correspondences(correspondences)
                .build();
    }
}
