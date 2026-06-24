package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures;

import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.impl.XClosureImplCustom;

public class XClosureFixtures {
    public static XClosure createXClosure() {
        return new XClosureImplCustom();
    }
}
