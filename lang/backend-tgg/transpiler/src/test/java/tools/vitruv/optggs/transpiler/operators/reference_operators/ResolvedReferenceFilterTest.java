package tools.vitruv.optggs.transpiler.operators.reference_operators;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.transpiler.graph.Attribute;
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
import java.util.Set;
import java.util.stream.Stream;

class ResolvedReferenceFilterTest extends AbstractTripleGraphGrammarTest {
    private static Stream<Arguments> extendRulesSource() {
        return Stream.of(
                Arguments.of(
                        "someIntField",
                        LogicOperator.Equals,
                        ConstantExpression.Primitive(123),
                        new Attribute(
                                "someIntField",
                                LogicOperator.Equals,
                                ConstantExpression.Primitive(123))),
                Arguments.of(
                        "someString",
                        LogicOperator.NotEquals,
                        ConstantExpression.String("SomeNotEqualsValue"),
                        new Attribute(
                                "someString",
                                LogicOperator.NotEquals,
                                ConstantExpression.String("SomeNotEqualsValue"))),
                Arguments.of(
                        "someFloatField",
                        LogicOperator.LessEquals,
                        ConstantExpression.Primitive(0.3F),
                        new Attribute(
                                "someFloatField",
                                LogicOperator.LessEquals,
                                ConstantExpression.Primitive(0.3F))),
                Arguments.of(
                        "double",
                        LogicOperator.MoreThan,
                        ConstantExpression.Primitive(19.4D),
                        new Attribute(
                                "double",
                                LogicOperator.MoreThan,
                                ConstantExpression.Primitive(19.4D))));
    }

    @ParameterizedTest
    @MethodSource("extendRulesSource")
    public void extendRules(
            String field,
            LogicOperator logicOperator,
            ConstantExpression expression,
            Attribute expectedAttribute) {
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

        final ResolvedReferenceFilter resolvedReferenceFilter =
                new ResolvedReferenceFilter(field, logicOperator, expression);

        // when
        resolvedReferenceFilter.extendRules(tripleRulesBuilder);

        // then
        assertThat(tripleRulesBuilder.getTripleRules()).hasSize(2);

        // Check that other rule is unchanged
        final TripleRule otherTripleRuleAfterUpdate =
                getTripleRuleById(tripleRulesBuilder, someOtherRule.getId());
        assertIsTestTripleRule(otherTripleRuleAfterUpdate);

        // Check that the attribute was added correctly
        final TripleRule extendedRule = getTripleRuleById(tripleRulesBuilder, ruleToExtend.getId());
        assertThat(extendedRule.allSourcesAsSlice().nodes()).hasSize(2);

        final TGGNode sourceChildNode =
                extendedRule.allSourcesAsSlice().nodes().stream()
                        .filter(node -> node.getId().equals("SomeOtherChildSourceNode"))
                        .findFirst()
                        .orElseThrow();
        assertThat(sourceChildNode.getId()).isEqualTo("SomeOtherChildSourceNode");
        assertThat(sourceChildNode.getType())
                .isEqualTo(new FQN("SomeOtherSourceMetamodel", "SomeOtherSourceLocalName1"));
        assertThat(sourceChildNode.isGreen()).isTrue();
        assertThat(sourceChildNode.links()).isEmpty();

        assertThat(sourceChildNode.attributes())
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        new Attribute(
                                "someSourceChildAttribute",
                                LogicOperator.Equals,
                                ConstantExpression.String("EqualsThis")),
                        expectedAttribute);

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
        assertThat(sourceParentNode.attributes())
                .containsExactly(
                        new Attribute(
                                "someSourceParentAttribute",
                                LogicOperator.LessThan,
                                ConstantExpression.Primitive(37)));

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
                        new LinkedHashSet<>(
                                Set.of(
                                        new Attribute(
                                                "someSourceChildAttribute",
                                                LogicOperator.Equals,
                                                ConstantExpression.String("EqualsThis")))));
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
                        new LinkedHashSet<>(
                                Set.of(
                                        new Attribute(
                                                "someSourceParentAttribute",
                                                LogicOperator.LessThan,
                                                ConstantExpression.Primitive(37)))));

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
