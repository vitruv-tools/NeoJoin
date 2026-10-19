package tools.vitruv.optggs.transpiler.operators.reference_operators;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.transpiler.graph.NameRepositoryFixtures;
import tools.vitruv.optggs.transpiler.graph.tgg.AbstractTripleGraphGrammarTest;
import tools.vitruv.optggs.transpiler.graph.tgg.Correspondence;
import tools.vitruv.optggs.transpiler.graph.tgg.GraphPathToNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGLink;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNodeFixtures;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRuleFixtures;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRulesBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

class ResolvedSingleMemberFeatureCallTest extends AbstractTripleGraphGrammarTest {

    @Test
    public void extendRules() {
        // given
        final TripleRule someOtherRule = getTestTripleRule();
        final TripleRule ruleToExtend = getTripleRuleToExtend();

        final TripleRulesBuilder tripleRulesBuilder = new TripleRulesBuilder();
        tripleRulesBuilder.addRule(someOtherRule);
        tripleRulesBuilder.addRule(ruleToExtend);
        tripleRulesBuilder.setPathToLastNode(
                new GraphPathToNode(
                        new FQN("SomeOtherSourceMetamodel", "SomeOtherSourceLocalName2"),
                        new ArrayList<>(List.of("someOtherSourceLinkFromParentToChild"))));

        final ResolvedSingleMemberFeatureCall resolvedSingleMemberFeatureCall =
                new ResolvedSingleMemberFeatureCall(
                        "someNewFeature", new FQN("NewNodeMetamodel", "NewNodeLocalName"));

        // when
        resolvedSingleMemberFeatureCall.extendRules(tripleRulesBuilder);

        // then
        assertThat(tripleRulesBuilder.getTripleRules()).hasSize(2);

        // Check that other rule is unchanged
        final TripleRule otherTripleRuleAfterUpdate =
                getTripleRuleById(tripleRulesBuilder, someOtherRule.getId());
        assertIsTestTripleRule(otherTripleRuleAfterUpdate);

        // Check that the rule was extended correctly
        final TripleRule extendedRule = getTripleRuleById(tripleRulesBuilder, ruleToExtend.getId());
        assertThat(extendedRule.allSourcesAsSlice().nodes()).hasSize(3);

        // New source node
        final TGGNode sourceChildChildNode =
                extendedRule.allSourcesAsSlice().nodes().stream()
                        .filter(node -> !node.getId().equals("SomeOtherChildSourceNode"))
                        .filter(node -> !node.getId().equals("SomeOtherParentSourceNode"))
                        .findFirst()
                        .orElseThrow();
        assertThat(sourceChildChildNode.getId())
                .isNotEqualTo("SomeOtherChildSourceNode")
                .isNotEqualTo("SomeOtherParentSourceNode")
                .isNotNull();
        assertThat(sourceChildChildNode.getType())
                .isEqualTo(new FQN("NewNodeMetamodel", "NewNodeLocalName"));
        assertThat(sourceChildChildNode.isGreen()).isTrue();
        assertThat(sourceChildChildNode.links()).isEmpty();
        assertThat(sourceChildChildNode.attributes()).isEmpty();

        final TGGNode sourceChildNode =
                extendedRule.allSourcesAsSlice().nodes().stream()
                        .filter(node -> node.getId().equals("SomeOtherChildSourceNode"))
                        .findFirst()
                        .orElseThrow();
        assertThat(sourceChildNode.getId()).isEqualTo("SomeOtherChildSourceNode");
        assertThat(sourceChildNode.getType())
                .isEqualTo(new FQN("SomeOtherSourceMetamodel", "SomeOtherSourceLocalName1"));
        assertThat(sourceChildNode.isGreen()).isTrue();
        assertThat(sourceChildNode.attributes()).isEmpty();

        // New link to new node
        assertThat(sourceChildNode.links()).hasSize(1);
        final TGGLink sourceChildNodeLinkToNewChild =
                sourceChildNode.links().stream().toList().getFirst();
        assertThat(sourceChildNodeLinkToNewChild.getName()).isEqualTo("someNewFeature");
        assertThat(sourceChildNodeLinkToNewChild.getTarget()).isEqualTo(sourceChildChildNode);
        assertThat(sourceChildNodeLinkToNewChild.isGreen()).isTrue();

        final TGGNode sourceParentNode =
                extendedRule.allSourcesAsSlice().nodes().stream()
                        .filter(node -> node.getId().equals("SomeOtherParentSourceNode"))
                        .findFirst()
                        .orElseThrow();
        assertThat(sourceParentNode.getId()).isEqualTo("SomeOtherParentSourceNode");
        assertThat(sourceParentNode.getType())
                .isEqualTo(new FQN("SomeOtherSourceMetamodel", "SomeOtherSourceLocalName2"));
        assertThat(sourceParentNode.isGreen()).isFalse();
        assertThat(sourceParentNode.links()).hasSize(1);
        final TGGLink sourceParentNodeExistingLinkToChild =
                sourceParentNode.links().stream()
                        .filter(
                                link ->
                                        link.getName()
                                                .equals("someOtherSourceLinkFromParentToChild"))
                        .findFirst()
                        .orElseThrow();
        assertThat(sourceParentNodeExistingLinkToChild.getName())
                .isEqualTo("someOtherSourceLinkFromParentToChild");
        assertThat(sourceParentNodeExistingLinkToChild.getTarget()).isEqualTo(sourceChildNode);
        assertThat(sourceParentNodeExistingLinkToChild.isGreen()).isTrue();
        assertThat(sourceParentNode.attributes()).isEmpty();

        // Assert target nodes
        assertThat(extendedRule.allTargetsAsSlice().nodes()).hasSize(1);

        final TGGNode targetNode =
                extendedRule.allTargetsAsSlice().nodes().stream().findFirst().orElseThrow();
        assertThat(targetNode.getId()).isEqualTo("SomeOtherTargetNode");
        assertThat(targetNode.getType())
                .isEqualTo(new FQN("SomeOtherTargetMetamodel", "SomeOtherTargetLocalName1"));
        assertThat(targetNode.isGreen()).isTrue();
        assertThat(targetNode.links()).isEmpty();
        assertThat(targetNode.attributes()).isEmpty();

        // Assert correspondences
        assertThat(extendedRule.correspondences()).hasSize(1);

        final Correspondence correspondence =
                extendedRule.correspondences().stream().findFirst().orElseThrow();
        assertThat(correspondence.getSource()).isEqualTo(sourceParentNode);
        assertThat(correspondence.getTarget()).isEqualTo(targetNode);
        assertThat(correspondence.isGreen()).isFalse();
    }

    private TripleRule getTripleRuleToExtend() {
        final TGGNode anotherSourceChildNode =
                TGGNodeFixtures.someTGGNode(
                        "SomeOtherChildSourceNode",
                        new FQN("SomeOtherSourceMetamodel", "SomeOtherSourceLocalName1"),
                        true,
                        NameRepositoryFixtures.someNameRepository(),
                        new ArrayList<>(),
                        new LinkedHashSet<>());
        final TGGNode anotherSourceParentNode =
                TGGNodeFixtures.someTGGNode(
                        "SomeOtherParentSourceNode",
                        new FQN("SomeOtherSourceMetamodel", "SomeOtherSourceLocalName2"),
                        false,
                        NameRepositoryFixtures.someNameRepository(),
                        new ArrayList<>(
                                List.of(
                                        TGGLink.Green(
                                                "someOtherSourceLinkFromParentToChild",
                                                anotherSourceChildNode))),
                        new LinkedHashSet<>());

        final TGGNode anotherTargetNode =
                TGGNodeFixtures.someTGGNode(
                        "SomeOtherTargetNode",
                        new FQN("SomeOtherTargetMetamodel", "SomeOtherTargetLocalName1"),
                        true,
                        NameRepositoryFixtures.someNameRepository(),
                        new ArrayList<>(),
                        new LinkedHashSet<>());

        final Correspondence anotherRuleCorrespondence =
                Correspondence.Black(anotherSourceParentNode, anotherTargetNode);

        return TripleRuleFixtures.someTripleRule(
                new ArrayList<>(List.of(anotherSourceChildNode, anotherSourceParentNode)),
                new ArrayList<>(List.of(anotherTargetNode)),
                new ArrayList<>(List.of(anotherRuleCorrespondence)));
    }
}
