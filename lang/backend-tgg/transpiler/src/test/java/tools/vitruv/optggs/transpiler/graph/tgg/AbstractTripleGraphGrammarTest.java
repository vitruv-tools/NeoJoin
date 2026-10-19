package tools.vitruv.optggs.transpiler.graph.tgg;

import static org.assertj.core.api.Assertions.assertThat;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.NameRepositoryFixtures;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public abstract class AbstractTripleGraphGrammarTest {
    protected static final String SOURCE_METAMODEL_NAME = "SourceMetamodel" + UUID.randomUUID();
    protected static final String Target_METAMODEL_NAME = "TargetMetamodel" + UUID.randomUUID();

    protected static final String SOURCE_CHILD_NODE_ID = "SomeChildSourceNode" + UUID.randomUUID();
    protected static final String SOURCE_PARENT_NODE_ID =
            "SomeParentSourceNode" + UUID.randomUUID();
    protected static final String TARGET_NODE_ID = "SomeTargetNode" + UUID.randomUUID();

    protected static final String SOURCE_CHILD_LOCAL_NAME = "SourceLocalName1" + UUID.randomUUID();
    protected static final String SOURCE_PARENT_LOCAL_NAME = "SourceLocalName2" + UUID.randomUUID();
    protected static final String TARGET_LOCAL_NAME = "TargetLocalName1" + UUID.randomUUID();

    protected static final String SOURCE_LINK_NAME =
            "someSourceNodeFromParentToChild" + UUID.randomUUID();

    public TripleRule getTestTripleRule() {
        final TGGNode sourceChildNode =
                TGGNodeFixtures.someTGGNode(
                        SOURCE_CHILD_NODE_ID,
                        new FQN(SOURCE_METAMODEL_NAME, SOURCE_CHILD_LOCAL_NAME),
                        false,
                        NameRepositoryFixtures.someNameRepository(),
                        new ArrayList<>(),
                        new LinkedHashSet<>());
        final TGGNode sourceParentNode =
                TGGNodeFixtures.someTGGNode(
                        SOURCE_PARENT_NODE_ID,
                        new FQN(SOURCE_METAMODEL_NAME, SOURCE_PARENT_LOCAL_NAME),
                        true,
                        NameRepositoryFixtures.someNameRepository(),
                        new ArrayList<>(List.of(TGGLink.Green(SOURCE_LINK_NAME, sourceChildNode))),
                        new LinkedHashSet<>());

        final TGGNode targetNode =
                TGGNodeFixtures.someTGGNode(
                        TARGET_NODE_ID,
                        new FQN(Target_METAMODEL_NAME, TARGET_LOCAL_NAME),
                        true,
                        NameRepositoryFixtures.someNameRepository(),
                        new ArrayList<>(),
                        new LinkedHashSet<>());

        final Correspondence someRuleCorrespondence =
                Correspondence.Green(sourceParentNode, targetNode);

        return TripleRuleFixtures.someTripleRule(
                new ArrayList<>(List.of(sourceChildNode, sourceParentNode)),
                new ArrayList<>(List.of(targetNode)),
                new ArrayList<>(List.of(someRuleCorrespondence)));
    }

    public void assertIsTestTripleRule(TripleRule tripleRule) {
        // Assert source nodes
        assertThat(tripleRule.allSourcesAsSlice().nodes()).hasSize(2);

        final TGGNode sourceChildNode =
                tripleRule.allSourcesAsSlice().nodes().stream()
                        .filter(node -> node.getId().equals(SOURCE_CHILD_NODE_ID))
                        .findFirst()
                        .orElseThrow();
        assertThat(sourceChildNode.getId()).isEqualTo(SOURCE_CHILD_NODE_ID);
        assertThat(sourceChildNode.getType())
                .isEqualTo(new FQN(SOURCE_METAMODEL_NAME, SOURCE_CHILD_LOCAL_NAME));
        assertThat(sourceChildNode.isGreen()).isFalse();
        assertThat(sourceChildNode.links()).isEmpty();
        assertThat(sourceChildNode.attributes()).isEmpty();

        final TGGNode sourceParentNode =
                tripleRule.allSourcesAsSlice().nodes().stream()
                        .filter(node -> node.getId().equals(SOURCE_PARENT_NODE_ID))
                        .findFirst()
                        .orElseThrow();
        assertThat(sourceParentNode.getId()).isEqualTo(SOURCE_PARENT_NODE_ID);
        assertThat(sourceParentNode.getType())
                .isEqualTo(new FQN(SOURCE_METAMODEL_NAME, SOURCE_PARENT_LOCAL_NAME));
        assertThat(sourceParentNode.isGreen()).isTrue();
        assertThat(sourceParentNode.links()).hasSize(1);
        assertThat(sourceParentNode.links().stream().toList().getFirst().getName())
                .isEqualTo(SOURCE_LINK_NAME);
        assertThat(sourceParentNode.links().stream().toList().getFirst().getTarget())
                .isEqualTo(sourceChildNode);
        assertThat(sourceParentNode.links().stream().toList().getFirst().isGreen()).isTrue();
        assertThat(sourceParentNode.attributes()).isEmpty();

        // Assert target nodes
        assertThat(tripleRule.allTargetsAsSlice().nodes()).hasSize(1);

        final TGGNode targetNode =
                tripleRule.allTargetsAsSlice().nodes().stream().findFirst().orElseThrow();
        assertThat(targetNode.getId()).isEqualTo(TARGET_NODE_ID);
        assertThat(targetNode.getType())
                .isEqualTo(new FQN(Target_METAMODEL_NAME, TARGET_LOCAL_NAME));
        assertThat(targetNode.isGreen()).isTrue();
        assertThat(targetNode.links()).isEmpty();
        assertThat(targetNode.attributes()).isEmpty();

        // Assert correspondences
        assertThat(tripleRule.correspondences()).hasSize(1);

        final Correspondence correspondence =
                tripleRule.correspondences().stream().findFirst().orElseThrow();
        assertThat(correspondence.getSource()).isEqualTo(sourceParentNode);
        assertThat(correspondence.getTarget()).isEqualTo(targetNode);
        assertThat(correspondence.isGreen()).isTrue();
    }

    protected TripleRule getTripleRuleById(TripleRulesBuilder builder, UUID id) {
        return getTripleRuleById(builder.getTripleRules(), id);
    }

    protected TripleRule getTripleRuleById(List<TripleRule> tripleRules, UUID id) {
        return tripleRules.stream()
                .filter(rule -> rule.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
