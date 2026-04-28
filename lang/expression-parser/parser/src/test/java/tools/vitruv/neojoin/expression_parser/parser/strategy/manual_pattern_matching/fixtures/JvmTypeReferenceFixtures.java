package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures;

import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.TypesFactory;

public class JvmTypeReferenceFixtures {
    public static JvmTypeReference createJvmTypeReference(JvmType type) {
        final JvmParameterizedTypeReference jvmTypeReference =
                TypesFactory.eINSTANCE.createJvmParameterizedTypeReference();
        jvmTypeReference.setType(type);
        return jvmTypeReference;
    }

    public static JvmParameterizedTypeReference createJvmParameterizedTypeReference() {
        return TypesFactory.eINSTANCE.createJvmParameterizedTypeReference();
    }
}
