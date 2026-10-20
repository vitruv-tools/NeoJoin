package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures;

import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.impl.XBlockExpressionImplCustom;

public class XBlockExpressionFixtures {
    public static XBlockExpression createXBlockExpression() {
        return new XBlockExpressionImplCustom();
    }
}
