package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures;

import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.TypesFactory;

public class JvmOperationFixtures {
    public static JvmOperation createJvmOperation() {
        return TypesFactory.eINSTANCE.createJvmOperation();
    }
}
