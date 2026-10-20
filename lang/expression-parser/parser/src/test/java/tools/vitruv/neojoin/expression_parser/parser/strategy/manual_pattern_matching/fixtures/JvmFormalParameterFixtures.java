package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures;

import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.TypesFactory;

public class JvmFormalParameterFixtures {
    public static JvmFormalParameter createJvmFormalParameter() {
        return TypesFactory.eINSTANCE.createJvmFormalParameter();
    }

    public static JvmFormalParameter createJvmFormalParameter(JvmTypeReference typeReference) {
        final JvmFormalParameter formalParameter = createJvmFormalParameter();
        formalParameter.setParameterType(typeReference);
        return formalParameter;
    }
}
