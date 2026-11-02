package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures;

import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.TypesFactory;

public class JvmTypeFixtures {
    public static JvmType createJvmType(String identifier, String simpleName) {
        final JvmGenericType jvmType = TypesFactory.eINSTANCE.createJvmGenericType();
        jvmType.setIdentifier(identifier);
        jvmType.setSimpleName(simpleName);
        return jvmType;
    }
}
