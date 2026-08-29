package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.junit.jupiter.api.Test;

import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.ManualPatternMatchingStrategy;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.JvmOperationFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XBlockExpressionFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XClosureFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XMemberFeatureCallFixtures;

import java.util.Optional;

class CollectReferencesParserTest implements ExpressionParserTest {
    private static final CollectReferencesParser parser = new CollectReferencesParser();

    @Test
    public void parseToList() throws UnsupportedReferenceExpressionException {
        // given
        final JvmOperation toListOperation = JvmOperationFixtures.createJvmOperation();
        toListOperation.setSimpleName("toList");

        final XClosure closure = XClosureFixtures.createXClosure();
        closure.setExpression(XBlockExpressionFixtures.createXBlockExpression());

        final XMemberFeatureCall memberFeatureCall =
                XMemberFeatureCallFixtures.createXMemberFeatureCall();
        memberFeatureCall.setFeature(toListOperation);
        memberFeatureCall.getMemberCallArguments().add(closure);
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
        assertNull(result);
    }
}
