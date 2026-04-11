package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.junit.jupiter.api.Test;

import tools.vitruv.neojoin.expression_parser.model.FeatureInformation;
import tools.vitruv.neojoin.expression_parser.model.FlatMap;
import tools.vitruv.neojoin.expression_parser.model.Map;
import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.ManualPatternMatchingStrategy;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XFeatureCallFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XMemberFeatureCallFixtures;

import java.util.Optional;

class MapParserTest implements ExpressionParserTest {
    private static final MapParser parser = new MapParser();

    @Test
    public void parseWithSimpleLambdaExpression() throws UnsupportedReferenceExpressionException {
        // given
        final XFeatureCall mapExpressionFeatureCall =
                XFeatureCallFixtures.featureCallWithEmptyFormalParameter();

        final XMemberFeatureCall mapExpressionMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToOneFieldXMemberFeatureCall(
                        "some.test.package.SomeMember",
                        "SomeMemberSimpleName",
                        "someMemberReference");
        mapExpressionMemberFeatureCall.setMemberCallTarget(mapExpressionFeatureCall);

        final XMemberFeatureCall mapMemberFeatureCall =
                XMemberFeatureCallFixtures.mapOperationMemberFeatureCall(
                        mapExpressionMemberFeatureCall);
        mapMemberFeatureCall.setMemberCallTarget(exampleExpressionChain());

        // when
        final ManualPatternMatchingStrategy strategy = new ManualPatternMatchingStrategy();
        final Optional<ReferenceOperator> resultOptional =
                parser.parse(strategy, mapMemberFeatureCall);

        // then
        assertTrue(resultOptional.isPresent());

        final ReferenceOperator result =
                assertExampleExpressionChainResultAndGetFollowingReferenceOperator(
                        resultOptional.get());
        assertInstanceOf(Map.class, result);

        final Map resultAsMap = (Map) result;
        assertEquals(
                new FeatureInformation(
                        "someMemberReference",
                        "SomeMemberSimpleName",
                        "some.test.package.SomeMember"),
                resultAsMap.getFeatureInformation());
        assertNull(resultAsMap.getFollowingOperator());
    }

    @Test
    public void parseWithMultipleOneToOneMemberFeatureCalls()
            throws UnsupportedReferenceExpressionException {
        // given
        final XFeatureCall mapExpressionFeatureCall =
                XFeatureCallFixtures.featureCallWithEmptyFormalParameter();

        final XMemberFeatureCall mapExpressionMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToOneFieldXMemberFeatureCall(
                        "some.test.package.SomeMember",
                        "SomeMemberSimpleName",
                        "someMemberReference");
        mapExpressionMemberFeatureCall.setMemberCallTarget(mapExpressionFeatureCall);

        final XMemberFeatureCall mapExpressionDeeperMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToOneFieldXMemberFeatureCall(
                        "some.other.package.SomeDeeperMember",
                        "SomeDeeperMemberSimpleName",
                        "someDeeperMemberReference");
        mapExpressionDeeperMemberFeatureCall.setMemberCallTarget(mapExpressionMemberFeatureCall);

        final XMemberFeatureCall mapExpressionDeepestMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToOneFieldXMemberFeatureCall(
                        "some.deep.package.SomeDeepestMember",
                        "SomeDeepestMemberSimpleName",
                        "someDeepestMemberReference");
        mapExpressionDeepestMemberFeatureCall.setMemberCallTarget(
                mapExpressionDeeperMemberFeatureCall);

        final XMemberFeatureCall mapMemberFeatureCall =
                XMemberFeatureCallFixtures.mapOperationMemberFeatureCall(
                        mapExpressionDeepestMemberFeatureCall);
        mapMemberFeatureCall.setMemberCallTarget(exampleExpressionChain());

        // when
        final ManualPatternMatchingStrategy strategy = new ManualPatternMatchingStrategy();
        final Optional<ReferenceOperator> resultOptional =
                parser.parse(strategy, mapMemberFeatureCall);

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
                        "some.test.package.SomeMember"),
                firstMapOperator.getFeatureInformation());

        final ReferenceOperator secondOperator = firstMapOperator.getFollowingOperator();
        assertInstanceOf(Map.class, secondOperator);

        final Map secondMapOperator = (Map) secondOperator;
        assertEquals(
                new FeatureInformation(
                        "someDeeperMemberReference",
                        "SomeDeeperMemberSimpleName",
                        "some.other.package.SomeDeeperMember"),
                secondMapOperator.getFeatureInformation());

        final ReferenceOperator thirdOperator = secondMapOperator.getFollowingOperator();
        assertInstanceOf(Map.class, thirdOperator);

        final Map thirdMapOperator = (Map) thirdOperator;
        assertEquals(
                new FeatureInformation(
                        "someDeepestMemberReference",
                        "SomeDeepestMemberSimpleName",
                        "some.deep.package.SomeDeepestMember"),
                thirdMapOperator.getFeatureInformation());

        assertNull(thirdMapOperator.getFollowingOperator());
    }

    @Test
    public void parseWithOneToOneAndOneToManyMemberFeatureCalls()
            throws UnsupportedReferenceExpressionException {
        // given
        final XFeatureCall mapExpressionFeatureCall =
                XFeatureCallFixtures.featureCallWithEmptyFormalParameter();

        final XMemberFeatureCall mapExpressionOneToManyMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToManyFieldXMemberFeatureCall(
                        "some.oneToMany.package.SomeMember",
                        "SomeMemberSimpleName",
                        "someMemberReference");
        mapExpressionOneToManyMemberFeatureCall.setMemberCallTarget(mapExpressionFeatureCall);

        final XMemberFeatureCall mapExpressionDeeperMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToOneFieldXMemberFeatureCall(
                        "some.oneToOne.package.SomeDeeperMember",
                        "SomeDeeperMemberSimpleName",
                        "someDeeperMemberReference");
        mapExpressionDeeperMemberFeatureCall.setMemberCallTarget(
                mapExpressionOneToManyMemberFeatureCall);

        final XMemberFeatureCall mapExpressionDeepestMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToManyFieldXMemberFeatureCall(
                        "some.oneToMany.package.SomeDeepestMember",
                        "SomeDeepestMemberSimpleName",
                        "someDeepestMemberReference");
        mapExpressionDeepestMemberFeatureCall.setMemberCallTarget(
                mapExpressionDeeperMemberFeatureCall);

        final XMemberFeatureCall mapMemberFeatureCall =
                XMemberFeatureCallFixtures.mapOperationMemberFeatureCall(
                        mapExpressionDeepestMemberFeatureCall);
        mapMemberFeatureCall.setMemberCallTarget(exampleExpressionChain());

        // when
        final ManualPatternMatchingStrategy strategy = new ManualPatternMatchingStrategy();
        final Optional<ReferenceOperator> resultOptional =
                parser.parse(strategy, mapMemberFeatureCall);

        // then
        assertTrue(resultOptional.isPresent());

        final ReferenceOperator result =
                assertExampleExpressionChainResultAndGetFollowingReferenceOperator(
                        resultOptional.get());
        assertInstanceOf(FlatMap.class, result);

        final FlatMap firstFlatMapOperator = (FlatMap) result;
        assertEquals(
                new FeatureInformation(
                        "someMemberReference",
                        "SomeMemberSimpleName",
                        "some.oneToMany.package.SomeMember"),
                firstFlatMapOperator.getFeatureInformation());

        final ReferenceOperator secondOperator = firstFlatMapOperator.getFollowingOperator();
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

    // TODO: Nested expressions (multiple layers) and filter operator
}
