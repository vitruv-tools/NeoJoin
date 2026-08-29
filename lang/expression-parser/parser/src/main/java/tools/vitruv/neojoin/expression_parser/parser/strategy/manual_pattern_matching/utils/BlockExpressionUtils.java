package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XExpression;

import java.util.Optional;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BlockExpressionUtils {
    public static boolean hasNoExpressions(XBlockExpression blockExpression) {
        return Optional.ofNullable(blockExpression)
                .map(XBlockExpression::getExpressions)
                .map(EList::isEmpty)
                .orElse(false);
    }

    public static boolean hasExactlyOneExpression(XBlockExpression blockExpression) {
        return Optional.ofNullable(blockExpression)
                .map(XBlockExpression::getExpressions)
                .map(expressions -> expressions.size() == 1)
                .orElse(false);
    }

    public static Optional<XExpression> getFirstExpression(XBlockExpression blockExpression) {
        return Optional.ofNullable(blockExpression)
                .map(XBlockExpression::getExpressions)
                .map(EList::stream)
                .flatMap(Stream::findFirst);
    }
}
