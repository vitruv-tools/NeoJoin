package tools.vitruv.neojoin.expression_parser.parser.strategy;

import org.eclipse.xtext.xbase.XExpression;
import org.jspecify.annotations.NonNull;

import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;

public interface PatternMatchingStrategy {
    @NonNull ReferenceOperator parseReferenceOperator(@NonNull XExpression expression)
            throws UnsupportedReferenceExpressionException;
}
