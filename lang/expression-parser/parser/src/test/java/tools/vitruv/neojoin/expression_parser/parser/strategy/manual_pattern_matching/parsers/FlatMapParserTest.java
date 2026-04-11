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
}
