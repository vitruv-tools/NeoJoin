package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures;

import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XbaseFactory;

public class XNumberLiteralFixtures {
    public static XNumberLiteral createXNumberLiteral() {
        return XbaseFactory.eINSTANCE.createXNumberLiteral();
    }

    public static XNumberLiteral XNumberLiteralWithValue(String value) {
        final XNumberLiteral numberLiteral = createXNumberLiteral();
        numberLiteral.setValue(value);
        return numberLiteral;
    }
}
