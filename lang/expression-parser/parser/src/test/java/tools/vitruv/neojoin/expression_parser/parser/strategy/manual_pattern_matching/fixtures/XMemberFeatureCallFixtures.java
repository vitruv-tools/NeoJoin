package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures;

import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.impl.XMemberFeatureCallImplCustom;

public class XMemberFeatureCallFixtures {
    public static XMemberFeatureCall createXMemberFeatureCall() {
        return new XMemberFeatureCallImplCustom();
    }

    public static XMemberFeatureCall simpleFieldXMemberFeatureCall(String simpleName) {
        final JvmField jvmField = JvmFieldFixtures.createJvmField();
        jvmField.setSimpleName(simpleName);

        final XMemberFeatureCall memberFeatureCall = createXMemberFeatureCall();
        memberFeatureCall.setFeature(jvmField);

        return memberFeatureCall;
    }

    public static XMemberFeatureCall oneToOneFieldXMemberFeatureCall(
            String identifier, String simpleName, String referenceName) {
        final JvmType jvmType = JvmTypeFixtures.createJvmType(identifier, simpleName);
        final JvmTypeReference typeReference =
                JvmTypeReferenceFixtures.createJvmTypeReference(jvmType);

        final JvmField jvmField = JvmFieldFixtures.createJvmField();
        jvmField.setSimpleName(referenceName);
        jvmField.setType(typeReference);

        final XMemberFeatureCall memberFeatureCall = createXMemberFeatureCall();
        memberFeatureCall.setFeature(jvmField);

        return memberFeatureCall;
    }

    public static XMemberFeatureCall oneToManyFieldXMemberFeatureCall(
            String identifier, String simpleName, String referenceName) {
        final JvmType listJvmType = JvmTypeFixtures.createJvmType("java.util.List", null);
        final JvmParameterizedTypeReference typeReference =
                JvmTypeReferenceFixtures.createJvmParameterizedTypeReference();
        typeReference.setType(listJvmType);

        final JvmType innerJvmType = JvmTypeFixtures.createJvmType(identifier, simpleName);
        final JvmTypeReference innerTypeReference =
                JvmTypeReferenceFixtures.createJvmTypeReference(innerJvmType);
        typeReference.getArguments().add(innerTypeReference);

        final JvmField jvmField = JvmFieldFixtures.createJvmField();
        jvmField.setSimpleName(referenceName);
        jvmField.setType(typeReference);

        final XMemberFeatureCall memberFeatureCall =
                XMemberFeatureCallFixtures.createXMemberFeatureCall();
        memberFeatureCall.setFeature(jvmField);

        return memberFeatureCall;
    }

    public static XMemberFeatureCall mapOperationMemberFeatureCall(XExpression innerExpression) {
        final XClosure closure = XClosureFixtures.createXClosure();
        final XBlockExpression blockExpression = XBlockExpressionFixtures.createXBlockExpression();
        blockExpression.getExpressions().add(innerExpression);
        closure.setExpression(blockExpression);

        final XMemberFeatureCall mapMemberFeatureCall =
                XMemberFeatureCallFixtures.createXMemberFeatureCall();
        mapMemberFeatureCall.getMemberCallArguments().add(closure);

        final JvmOperation mapOperation = JvmOperationFixtures.createJvmOperation();
        mapOperation.setSimpleName("map");
        mapMemberFeatureCall.setFeature(mapOperation);

        return mapMemberFeatureCall;
    }

    public static XMemberFeatureCall flatMapOperationMemberFeatureCall(
            XExpression innerExpression) {
        final XClosure closure = XClosureFixtures.createXClosure();
        final XBlockExpression blockExpression = XBlockExpressionFixtures.createXBlockExpression();
        blockExpression.getExpressions().add(innerExpression);
        closure.setExpression(blockExpression);

        final XMemberFeatureCall flatMapMemberFeatureCall =
                XMemberFeatureCallFixtures.createXMemberFeatureCall();
        flatMapMemberFeatureCall.getMemberCallArguments().add(closure);

        final JvmOperation flatMapOperation = JvmOperationFixtures.createJvmOperation();
        flatMapOperation.setSimpleName("flatMap");
        flatMapMemberFeatureCall.setFeature(flatMapOperation);

        return flatMapMemberFeatureCall;
    }

    public static XMemberFeatureCall filterOperationMemberFeatureCall(XExpression innerExpression) {
        final XClosure closure = XClosureFixtures.createXClosure();
        final XBlockExpression blockExpression = XBlockExpressionFixtures.createXBlockExpression();
        blockExpression.getExpressions().add(innerExpression);
        closure.setExpression(blockExpression);

        final XMemberFeatureCall filterMemberFeatureCall =
                XMemberFeatureCallFixtures.createXMemberFeatureCall();
        filterMemberFeatureCall.getMemberCallArguments().add(closure);

        final JvmOperation filterOperation = JvmOperationFixtures.createJvmOperation();
        filterOperation.setSimpleName("filter");
        filterMemberFeatureCall.setFeature(filterOperation);

        return filterMemberFeatureCall;
    }
}
