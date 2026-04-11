package tools.vitruv.neojoin.expression_parser.parser.exception;

import org.eclipse.xtext.xbase.XExpression;

public class UnsupportedReferenceExpressionException extends Exception {
    public static UnsupportedReferenceExpressionException fromExpression(XExpression expression) {
        return new UnsupportedReferenceExpressionException(
                String.format("The expression %s is not supported", expression));
    }

    public UnsupportedReferenceExpressionException(String message) {
        super(message);
    }

    public UnsupportedReferenceExpressionException(String message, XExpression expression) {
        super(String.format(message + ". The expression was %s", expression));
    }
}
