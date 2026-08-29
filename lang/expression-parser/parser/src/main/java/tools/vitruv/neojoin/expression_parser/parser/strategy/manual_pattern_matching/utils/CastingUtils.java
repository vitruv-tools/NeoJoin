package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CastingUtils {
    public static <T> Optional<T> cast(Object object, Class<T> clazz) {
        return Optional.ofNullable(object).filter(clazz::isInstance).map(clazz::cast);
    }

    public static Optional<XBlockExpression> asBlockExpression(XExpression expression) {
        return cast(expression, XBlockExpression.class);
    }

    public static Optional<XClosure> asClosure(XExpression expression) {
        return cast(expression, XClosure.class);
    }

    public static Optional<XMemberFeatureCall> asMemberFeatureCall(XExpression expression) {
        return cast(expression, XMemberFeatureCall.class);
    }

    public static Optional<XFeatureCall> asFeatureCall(XExpression expression) {
        return cast(expression, XFeatureCall.class);
    }

    public static Optional<XAbstractFeatureCall> asAbstractFeatureCall(XExpression expression) {
        return cast(expression, XAbstractFeatureCall.class);
    }

    public static Optional<JvmField> asJvmField(JvmIdentifiableElement jvmIdentifiableElement) {
        return cast(jvmIdentifiableElement, JvmField.class);
    }

    public static Optional<JvmOperation> asJvmOperation(
            JvmIdentifiableElement jvmIdentifiableElement) {
        return cast(jvmIdentifiableElement, JvmOperation.class);
    }

    public static Optional<JvmFormalParameter> asJvmFormalParameter(
            JvmIdentifiableElement jvmIdentifiableElement) {
        return cast(jvmIdentifiableElement, JvmFormalParameter.class);
    }

    public static Optional<JvmParameterizedTypeReference> asParameterizedTypeReference(
            JvmTypeReference typeReference) {
        return cast(typeReference, JvmParameterizedTypeReference.class);
    }

    public static Optional<XBinaryOperation> asBinaryOperation(XExpression expression) {
        return cast(expression, XBinaryOperation.class);
    }
}
