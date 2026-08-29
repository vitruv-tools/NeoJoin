package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.junit.jupiter.api.Test;

import tools.vitruv.neojoin.expression_parser.model.ReferenceFilter;
import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.model.predicate_expression.ComparisonOperator;
import tools.vitruv.neojoin.expression_parser.model.predicate_expression.ConstantValue;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.ManualPatternMatchingStrategy;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.JvmOperationFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XBinaryOperationFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XMemberFeatureCallFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XNumberLiteralFixtures;

import java.util.Optional;

class FilterParserTest implements ExpressionParserTest {
    private static final FilterParser parser = new FilterParser();

    @Test
    public void parse() throws UnsupportedReferenceExpressionException {
        // given
        XMemberFeatureCall leftOperand =
                XMemberFeatureCallFixtures.simpleFieldXMemberFeatureCall("someField");
        JvmOperation comparisonOperator = JvmOperationFixtures.createJvmOperation();
        comparisonOperator.setSimpleName("operator_lessThan");
        XNumberLiteral rightOperand = XNumberLiteralFixtures.XNumberLiteralWithValue("2.1");

        XBinaryOperation binaryOperation =
                XBinaryOperationFixtures.binaryOperation(
                        leftOperand, comparisonOperator, rightOperand);

        final XMemberFeatureCall filterMemberFeatureCall =
                XMemberFeatureCallFixtures.filterOperationMemberFeatureCall(binaryOperation);
        filterMemberFeatureCall.setMemberCallTarget(exampleExpressionChain());

        // when
        final ManualPatternMatchingStrategy strategy = new ManualPatternMatchingStrategy();
        final Optional<ReferenceOperator> resultOptional =
                parser.parse(strategy, filterMemberFeatureCall);

        // then
        assertTrue(resultOptional.isPresent());

        final ReferenceOperator result =
                assertExampleExpressionChainResultAndGetFollowingReferenceOperator(
                        resultOptional.get());
        assertInstanceOf(ReferenceFilter.class, result);

        final ReferenceFilter resultAsFilter = (ReferenceFilter) result;
        assertEquals("someField", resultAsFilter.getFeature());
        assertEquals(ComparisonOperator.LessThan, resultAsFilter.getOperator());
        assertEquals(ConstantValue.of("2.1"), resultAsFilter.getConstantValue());
        assertNull(resultAsFilter.getFollowingOperator());
    }
}
