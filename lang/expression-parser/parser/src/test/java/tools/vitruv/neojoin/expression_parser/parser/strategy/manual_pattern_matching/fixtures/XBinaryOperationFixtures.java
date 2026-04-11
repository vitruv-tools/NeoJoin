package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures;

import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.impl.XBinaryOperationImplCustom;

public class XBinaryOperationFixtures {
    public static XBinaryOperation createXBinaryOperation() {
        return new XBinaryOperationImplCustom();
    }

    public static XBinaryOperation binaryOperation(
            XExpression leftOperand, JvmIdentifiableElement feature, XExpression rightOperand) {
        final XBinaryOperation binaryOperation = createXBinaryOperation();
        binaryOperation.setLeftOperand(leftOperand);
        binaryOperation.setFeature(feature);
        binaryOperation.setRightOperand(rightOperand);
        return binaryOperation;
    }
}
