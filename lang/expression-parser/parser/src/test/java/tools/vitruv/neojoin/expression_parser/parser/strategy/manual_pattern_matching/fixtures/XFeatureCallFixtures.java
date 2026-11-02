package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures;

import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.impl.XFeatureCallImplCustom;

public class XFeatureCallFixtures {
    public static XFeatureCall createXFeatureCall() {
        return new XFeatureCallImplCustom();
    }

    public static XFeatureCall featureCallWithEmptyFormalParameter() {
        final JvmFormalParameter emptyFormalParameter =
                JvmFormalParameterFixtures.createJvmFormalParameter();
        final XFeatureCall featureCall = XFeatureCallFixtures.createXFeatureCall();
        featureCall.setFeature(emptyFormalParameter);
        return featureCall;
    }

    public static XFeatureCall featureCall(String identifier, String simpleName) {
        final JvmType jvmType = JvmTypeFixtures.createJvmType(identifier, simpleName);
        final JvmTypeReference jvmTypeReference =
                JvmTypeReferenceFixtures.createJvmTypeReference(jvmType);
        final JvmFormalParameter formalParameter =
                JvmFormalParameterFixtures.createJvmFormalParameter(jvmTypeReference);
        final XFeatureCall featureCall = XFeatureCallFixtures.createXFeatureCall();
        featureCall.setFeature(formalParameter);
        return featureCall;
    }
}
