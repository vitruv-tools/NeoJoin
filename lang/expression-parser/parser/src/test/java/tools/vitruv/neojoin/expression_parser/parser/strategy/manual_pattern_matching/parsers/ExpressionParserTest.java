package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

import tools.vitruv.neojoin.expression_parser.model.FeatureCall;
import tools.vitruv.neojoin.expression_parser.model.FeatureInformation;
import tools.vitruv.neojoin.expression_parser.model.Map;
import tools.vitruv.neojoin.expression_parser.model.MemberFeatureCall;
import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XFeatureCallFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XMemberFeatureCallFixtures;

interface ExpressionParserTest {
    default XMemberFeatureCall exampleExpressionChain() {
        final XFeatureCall featureCall =
                XFeatureCallFixtures.featureCall("some.feature.Call", "SomeCall");

        final XMemberFeatureCall oneToOneMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToOneFieldXMemberFeatureCall(
                        "some.oneToOne.member", "MemberFeatureCall", "oneToOneReference");
        oneToOneMemberFeatureCall.setMemberCallTarget(featureCall);

        final XMemberFeatureCall oneToManyMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToManyFieldXMemberFeatureCall(
                        "some.oneToMany.member", "ManyMembers", "oneToManyRef");
        oneToManyMemberFeatureCall.setMemberCallTarget(oneToOneMemberFeatureCall);

        final XFeatureCall mapExpressionFeatureCall =
                XFeatureCallFixtures.featureCallWithEmptyFormalParameter();
        final XMemberFeatureCall mapExpressionMemberFeatureCall =
                XMemberFeatureCallFixtures.oneToOneFieldXMemberFeatureCall(
                        "some.test.package.SomeMapMemberFeatureCall",
                        "SomeMapMemberSimpleName",
                        "someMapMemberReference");
        mapExpressionMemberFeatureCall.setMemberCallTarget(mapExpressionFeatureCall);
        final XMemberFeatureCall mapOperationMemberFeatureCall =
                XMemberFeatureCallFixtures.mapOperationMemberFeatureCall(
                        mapExpressionMemberFeatureCall);
        mapOperationMemberFeatureCall.setMemberCallTarget(oneToManyMemberFeatureCall);

        return mapOperationMemberFeatureCall;
    }

    default ReferenceOperator assertExampleExpressionChainResultAndGetFollowingReferenceOperator(
            ReferenceOperator referenceOperator) {
        assertInstanceOf(FeatureCall.class, referenceOperator);
        final FeatureCall firstFeatureCall = (FeatureCall) referenceOperator;
        assertEquals("some.feature.Call", firstFeatureCall.getIdentifier());
        assertEquals("SomeCall", firstFeatureCall.getSimpleName());

        final ReferenceOperator secondReferenceOperator = firstFeatureCall.getFollowingOperator();
        assertInstanceOf(MemberFeatureCall.class, secondReferenceOperator);
        final MemberFeatureCall oneToOneMemberFeatureCall =
                (MemberFeatureCall) secondReferenceOperator;
        assertEquals(
                new FeatureInformation(
                        "oneToOneReference", "MemberFeatureCall", "some.oneToOne.member"),
                oneToOneMemberFeatureCall.getFeatureInformation());
        assertFalse(oneToOneMemberFeatureCall.isCollection());

        final ReferenceOperator thirdReferenceOperator =
                oneToOneMemberFeatureCall.getFollowingOperator();
        assertInstanceOf(MemberFeatureCall.class, thirdReferenceOperator);
        final MemberFeatureCall oneToManyMemberFeatureCall =
                (MemberFeatureCall) thirdReferenceOperator;
        assertEquals(
                new FeatureInformation("oneToManyRef", "ManyMembers", "some.oneToMany.member"),
                oneToManyMemberFeatureCall.getFeatureInformation());
        assertTrue(oneToManyMemberFeatureCall.isCollection());

        final ReferenceOperator fourthReferenceOperator =
                oneToManyMemberFeatureCall.getFollowingOperator();
        assertInstanceOf(Map.class, fourthReferenceOperator);
        final Map map = (Map) fourthReferenceOperator;
        assertEquals(
                new FeatureInformation(
                        "someMapMemberReference",
                        "SomeMapMemberSimpleName",
                        "some.test.package.SomeMapMemberFeatureCall"),
                map.getFeatureInformation());

        return map.getFollowingOperator();
    }
}
