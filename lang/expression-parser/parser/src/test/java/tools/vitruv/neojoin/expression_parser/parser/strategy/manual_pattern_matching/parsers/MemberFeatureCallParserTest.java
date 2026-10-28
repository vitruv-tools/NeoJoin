package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.junit.jupiter.api.Test;

import tools.vitruv.neojoin.expression_parser.model.FeatureInformation;
import tools.vitruv.neojoin.expression_parser.model.MemberFeatureCall;
import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.ManualPatternMatchingStrategy;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.JvmFieldFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.JvmTypeFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.JvmTypeReferenceFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XMemberFeatureCallFixtures;

import java.util.Optional;

class MemberFeatureCallParserTest implements ExpressionParserTest {
    private static final MemberFeatureCallParser parser = new MemberFeatureCallParser();

    @Test
    public void parseOneToOneField() throws UnsupportedReferenceExpressionException {
        // given
        final JvmType jvmType =
                JvmTypeFixtures.createJvmType(
                        "my.test.package.SomeChildField", "SomeChildFieldSimpleName");
        final JvmTypeReference typeReference =
                JvmTypeReferenceFixtures.createJvmTypeReference(jvmType);

        final JvmField jvmField = JvmFieldFixtures.createJvmField();
        jvmField.setSimpleName("someChildFieldReference");
        jvmField.setType(typeReference);

        final XMemberFeatureCall memberFeatureCall =
                XMemberFeatureCallFixtures.createXMemberFeatureCall();
        memberFeatureCall.setFeature(jvmField);
        memberFeatureCall.setMemberCallTarget(exampleExpressionChain());

        // when
        final ManualPatternMatchingStrategy strategy = new ManualPatternMatchingStrategy();
        final Optional<ReferenceOperator> resultOptional =
                parser.parse(strategy, memberFeatureCall);

        // then
        assertTrue(resultOptional.isPresent());

        final ReferenceOperator result =
                assertExampleExpressionChainResultAndGetFollowingReferenceOperator(
                        resultOptional.get());
        assertInstanceOf(MemberFeatureCall.class, result);

        final MemberFeatureCall resultAsMemberFeatureCall = (MemberFeatureCall) result;
        assertEquals(
                new FeatureInformation(
                        "someChildFieldReference",
                        "SomeChildFieldSimpleName",
                        "my.test.package.SomeChildField"),
                resultAsMemberFeatureCall.getFeatureInformation());
        assertFalse(resultAsMemberFeatureCall.isCollection());
        assertNull(resultAsMemberFeatureCall.getFollowingOperator());
    }

    @Test
    public void parseListField() throws UnsupportedReferenceExpressionException {
        // given
        final JvmType listJvmType = JvmTypeFixtures.createJvmType("java.util.List", null);
        final JvmParameterizedTypeReference typeReference =
                JvmTypeReferenceFixtures.createJvmParameterizedTypeReference();
        typeReference.setType(listJvmType);

        final JvmType innerJvmType =
                JvmTypeFixtures.createJvmType(
                        "my.test.package.SomeChildField", "SomeChildFieldSimpleName");
        final JvmTypeReference innerTypeReference =
                JvmTypeReferenceFixtures.createJvmTypeReference(innerJvmType);
        typeReference.getArguments().add(innerTypeReference);

        final JvmField jvmField = JvmFieldFixtures.createJvmField();
        jvmField.setSimpleName("someChildFieldReference");
        jvmField.setType(typeReference);

        final XMemberFeatureCall memberFeatureCall =
                XMemberFeatureCallFixtures.createXMemberFeatureCall();
        memberFeatureCall.setFeature(jvmField);
        memberFeatureCall.setMemberCallTarget(exampleExpressionChain());

        // when
        final ManualPatternMatchingStrategy strategy = new ManualPatternMatchingStrategy();
        final Optional<ReferenceOperator> resultOptional =
                parser.parse(strategy, memberFeatureCall);

        // then
        assertTrue(resultOptional.isPresent());

        final ReferenceOperator result =
                assertExampleExpressionChainResultAndGetFollowingReferenceOperator(
                        resultOptional.get());
        assertInstanceOf(MemberFeatureCall.class, result);

        final MemberFeatureCall resultAsMemberFeatureCall = (MemberFeatureCall) result;
        assertEquals(
                new FeatureInformation(
                        "someChildFieldReference",
                        "SomeChildFieldSimpleName",
                        "my.test.package.SomeChildField"),
                resultAsMemberFeatureCall.getFeatureInformation());
        assertTrue(resultAsMemberFeatureCall.isCollection());
        assertNull(resultAsMemberFeatureCall.getFollowingOperator());
    }

    @Test
    public void shouldThrowWhenTypeIsNull() {
        // given
        final JvmField jvmField = JvmFieldFixtures.createJvmField();
        jvmField.setSimpleName("someChildFieldReference");
        jvmField.setType(null);

        final XMemberFeatureCall memberFeatureCall =
                XMemberFeatureCallFixtures.createXMemberFeatureCall();
        memberFeatureCall.setFeature(jvmField);

        // when & then
        final ManualPatternMatchingStrategy strategy = new ManualPatternMatchingStrategy();
        UnsupportedReferenceExpressionException exception =
                assertThrows(
                        UnsupportedReferenceExpressionException.class,
                        () -> parser.parse(strategy, memberFeatureCall));
        assertEquals("The MemberFeatureCall couldn't be parsed", exception.getMessage());
    }
}
