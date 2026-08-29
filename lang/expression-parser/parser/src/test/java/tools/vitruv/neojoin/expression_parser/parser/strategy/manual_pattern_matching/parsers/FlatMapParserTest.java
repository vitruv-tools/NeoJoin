package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.junit.jupiter.api.Test;

import tools.vitruv.neojoin.expression_parser.model.FeatureInformation;
import tools.vitruv.neojoin.expression_parser.model.FlatMap;
import tools.vitruv.neojoin.expression_parser.model.Map;
import tools.vitruv.neojoin.expression_parser.model.ReferenceFilter;
import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.model.predicate_expression.ComparisonOperator;
import tools.vitruv.neojoin.expression_parser.model.predicate_expression.ConstantValue;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.ManualPatternMatchingStrategy;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.JvmOperationFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XBinaryOperationFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XFeatureCallFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XMemberFeatureCallFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XNumberLiteralFixtures;

import java.util.Optional;

class FlatMapParserTest implements ExpressionParserTest {
    private static final FlatMapParser parser = new FlatMapParser();

    @Test
    public void parseWithSimpleLambdaExpression() throws UnsupportedReferenceExpressionException {
        // given
        final XFeatureCall flatMapExpressionFeatureCall =
                XFeatureCallFixtures.featureCallWithEmptyFormalParameter();

        final XMemberFeatureCall flatMapExpressionMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToManyFieldXMemberFeatureCall(
                        "some.test.package.SomeMember",
                        "SomeMemberSimpleName",
                        "someMemberReference");
        flatMapExpressionMemberFeatureCall.setMemberCallTarget(flatMapExpressionFeatureCall);

        final XMemberFeatureCall flatMapMemberFeatureCall =
                XMemberFeatureCallFixtures.flatMapOperationMemberFeatureCall(
                        flatMapExpressionMemberFeatureCall);
        flatMapMemberFeatureCall.setMemberCallTarget(exampleExpressionChain());

        // when
        final ManualPatternMatchingStrategy strategy = new ManualPatternMatchingStrategy();
        final Optional<ReferenceOperator> resultOptional =
                parser.parse(strategy, flatMapMemberFeatureCall);

        // then
        assertTrue(resultOptional.isPresent());

        final ReferenceOperator result =
                assertExampleExpressionChainResultAndGetFollowingReferenceOperator(
                        resultOptional.get());
        assertInstanceOf(FlatMap.class, result);

        final FlatMap resultAsFlatMap = (FlatMap) result;
        assertEquals(
                new FeatureInformation(
                        "someMemberReference",
                        "SomeMemberSimpleName",
                        "some.test.package.SomeMember"),
                resultAsFlatMap.getFeatureInformation());
        assertNull(resultAsFlatMap.getFollowingOperator());
    }

    @Test
    public void parseWithOneToOneAndOneToManyMemberFeatureCalls()
            throws UnsupportedReferenceExpressionException {
        // given
        final XFeatureCall flatMapExpressionFeatureCall =
                XFeatureCallFixtures.featureCallWithEmptyFormalParameter();

        final XMemberFeatureCall flatMapExpressionOneToOneMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToOneFieldXMemberFeatureCall(
                        "some.oneToOne.package.SomeMember",
                        "SomeMemberSimpleName",
                        "someMemberReference");
        flatMapExpressionOneToOneMemberFeatureCall.setMemberCallTarget(
                flatMapExpressionFeatureCall);

        final XMemberFeatureCall flatMapExpressionDeeperMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToOneFieldXMemberFeatureCall(
                        "some.oneToOne.package.SomeDeeperMember",
                        "SomeDeeperMemberSimpleName",
                        "someDeeperMemberReference");
        flatMapExpressionDeeperMemberFeatureCall.setMemberCallTarget(
                flatMapExpressionOneToOneMemberFeatureCall);

        final XMemberFeatureCall flatMapExpressionDeepestMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToManyFieldXMemberFeatureCall(
                        "some.oneToMany.package.SomeDeepestMember",
                        "SomeDeepestMemberSimpleName",
                        "someDeepestMemberReference");
        flatMapExpressionDeepestMemberFeatureCall.setMemberCallTarget(
                flatMapExpressionDeeperMemberFeatureCall);

        final XMemberFeatureCall flatMapMemberFeatureCall =
                XMemberFeatureCallFixtures.flatMapOperationMemberFeatureCall(
                        flatMapExpressionDeepestMemberFeatureCall);
        flatMapMemberFeatureCall.setMemberCallTarget(exampleExpressionChain());

        // when
        final ManualPatternMatchingStrategy strategy = new ManualPatternMatchingStrategy();
        final Optional<ReferenceOperator> resultOptional =
                parser.parse(strategy, flatMapMemberFeatureCall);

        // then
        assertTrue(resultOptional.isPresent());

        final ReferenceOperator result =
                assertExampleExpressionChainResultAndGetFollowingReferenceOperator(
                        resultOptional.get());
        assertInstanceOf(Map.class, result);

        final Map firstMapOperator = (Map) result;
        assertEquals(
                new FeatureInformation(
                        "someMemberReference",
                        "SomeMemberSimpleName",
                        "some.oneToOne.package.SomeMember"),
                firstMapOperator.getFeatureInformation());

        final ReferenceOperator secondOperator = firstMapOperator.getFollowingOperator();
        assertInstanceOf(Map.class, secondOperator);

        final Map secondMapOperator = (Map) secondOperator;
        assertEquals(
                new FeatureInformation(
                        "someDeeperMemberReference",
                        "SomeDeeperMemberSimpleName",
                        "some.oneToOne.package.SomeDeeperMember"),
                secondMapOperator.getFeatureInformation());

        final ReferenceOperator thirdOperator = secondMapOperator.getFollowingOperator();
        assertInstanceOf(FlatMap.class, thirdOperator);

        final FlatMap thirdFlatMapOperator = (FlatMap) thirdOperator;
        assertEquals(
                new FeatureInformation(
                        "someDeepestMemberReference",
                        "SomeDeepestMemberSimpleName",
                        "some.oneToMany.package.SomeDeepestMember"),
                thirdFlatMapOperator.getFeatureInformation());

        assertNull(thirdFlatMapOperator.getFollowingOperator());
    }

    @Test
    public void parseWithNestedFlatMapContainingFilterAndMap()
            throws UnsupportedReferenceExpressionException {
        // given: outer flatMap's closure body is itself another flatMap operation whose inner
        // closure contains a filter and a map, i.e.
        // `flatMap(x -> x.flatMap(y -> y.someList.filter(someField < 2.1).map(z -> z.nestedRef)))`

        // Innermost closure body for the nested map: `z.nestedRef`
        final XFeatureCall nestedMapInnerFeatureCall =
                XFeatureCallFixtures.featureCallWithEmptyFormalParameter();

        final XMemberFeatureCall nestedMapInnerMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToOneFieldXMemberFeatureCall(
                        "some.nested.package.NestedMember",
                        "NestedMemberSimpleName",
                        "nestedMemberReference");
        nestedMapInnerMemberFeatureCall.setMemberCallTarget(nestedMapInnerFeatureCall);

        // Filter predicate `someField < 2.1` used inside the inner flatMap's closure
        final XMemberFeatureCall filterLeftOperand =
                XMemberFeatureCallFixtures.simpleFieldXMemberFeatureCall("someField");
        final JvmOperation comparisonOperator = JvmOperationFixtures.createJvmOperation();
        comparisonOperator.setSimpleName("operator_lessThan");
        final XNumberLiteral filterRightOperand =
                XNumberLiteralFixtures.XNumberLiteralWithValue("2.1");
        final XBinaryOperation binaryOperation =
                XBinaryOperationFixtures.binaryOperation(
                        filterLeftOperand, comparisonOperator, filterRightOperand);

        // Inner flatMap closure body: `y.someList.filter(...).map(z -> z.nestedRef)`
        final XFeatureCall innerFlatMapLambdaParameter =
                XFeatureCallFixtures.featureCallWithEmptyFormalParameter();

        final XMemberFeatureCall someListMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToManyFieldXMemberFeatureCall(
                        "some.nested.package.SomeList",
                        "SomeListSimpleName",
                        "someListReference");
        someListMemberFeatureCall.setMemberCallTarget(innerFlatMapLambdaParameter);

        final XMemberFeatureCall innerFilterMemberFeatureCall =
                XMemberFeatureCallFixtures.filterOperationMemberFeatureCall(binaryOperation);
        innerFilterMemberFeatureCall.setMemberCallTarget(someListMemberFeatureCall);

        final XMemberFeatureCall innerMapMemberFeatureCall =
                XMemberFeatureCallFixtures.mapOperationMemberFeatureCall(
                        nestedMapInnerMemberFeatureCall);
        innerMapMemberFeatureCall.setMemberCallTarget(innerFilterMemberFeatureCall);

        // Inner flatMap call: `x.flatMap(y -> ...)` — its target is the outer flatMap lambda
        // parameter `x`, which is itself treated as a collection that flatMap operates on
        final XFeatureCall outerFlatMapLambdaParameter =
                XFeatureCallFixtures.featureCallWithEmptyFormalParameter();

        final XMemberFeatureCall innerFlatMapMemberFeatureCall =
                XMemberFeatureCallFixtures.flatMapOperationMemberFeatureCall(
                        innerMapMemberFeatureCall);
        innerFlatMapMemberFeatureCall.setMemberCallTarget(outerFlatMapLambdaParameter);

        // Outer flatMap call whose closure body is the inner flatMap call above
        final XMemberFeatureCall flatMapMemberFeatureCall =
                XMemberFeatureCallFixtures.flatMapOperationMemberFeatureCall(
                        innerFlatMapMemberFeatureCall);
        flatMapMemberFeatureCall.setMemberCallTarget(exampleExpressionChain());

        // when
        final ManualPatternMatchingStrategy strategy = new ManualPatternMatchingStrategy();
        final Optional<ReferenceOperator> resultOptional =
                parser.parse(strategy, flatMapMemberFeatureCall);

        // then
        assertTrue(resultOptional.isPresent());

        final ReferenceOperator result =
                assertExampleExpressionChainResultAndGetFollowingReferenceOperator(
                        resultOptional.get());
        assertInstanceOf(FlatMap.class, result);

        final FlatMap flatMapOperator = (FlatMap) result;
        assertEquals(
                new FeatureInformation(
                        "someListReference",
                        "SomeListSimpleName",
                        "some.nested.package.SomeList"),
                flatMapOperator.getFeatureInformation());

        final ReferenceOperator secondOperator = flatMapOperator.getFollowingOperator();
        assertInstanceOf(ReferenceFilter.class, secondOperator);

        final ReferenceFilter filterOperator = (ReferenceFilter) secondOperator;
        assertEquals("someField", filterOperator.getFeature());
        assertEquals(ComparisonOperator.LessThan, filterOperator.getOperator());
        assertEquals(ConstantValue.of("2.1"), filterOperator.getConstantValue());

        final ReferenceOperator thirdOperator = filterOperator.getFollowingOperator();
        assertInstanceOf(Map.class, thirdOperator);

        final Map mapOperator = (Map) thirdOperator;
        assertEquals(
                new FeatureInformation(
                        "nestedMemberReference",
                        "NestedMemberSimpleName",
                        "some.nested.package.NestedMember"),
                mapOperator.getFeatureInformation());

        assertNull(mapOperator.getFollowingOperator());
    }
}
