package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

import tools.vitruv.neojoin.expression_parser.model.FeatureInformation;
import tools.vitruv.neojoin.expression_parser.model.MemberFeatureCall;
import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.PatternMatchingStrategy;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.CastingUtils;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils.JvmTypeReferenceUtils;

import java.util.Optional;

public class MemberFeatureCallParser implements ReferenceOperatorParser {
    private static final String LIST_IDENTIFIER = "java.util.List";

    public Optional<ReferenceOperator> parse(
            PatternMatchingStrategy strategy, XExpression expression)
            throws UnsupportedReferenceExpressionException {
        final Optional<XMemberFeatureCall> memberFeatureCall =
                CastingUtils.asMemberFeatureCall(expression);
        if (memberFeatureCall.isEmpty()) {
            return Optional.empty();
        }

        final Optional<JvmField> jvmField =
                memberFeatureCall
                        .map(XAbstractFeatureCall::getFeature)
                        .flatMap(CastingUtils::asJvmField);
        if (jvmField.isEmpty()) {
            return Optional.empty();
        }

        final Optional<ReferenceOperator> foundOperatorOptional;
        if (MemberFeatureCallParser.isListType(jvmField.get().getType())) {
            foundOperatorOptional =
                    jvmField.flatMap(MemberFeatureCallParser::getListFeatureInformation)
                            .map(
                                    featureInformation ->
                                            new MemberFeatureCall(featureInformation, true));
        } else {
            foundOperatorOptional =
                    jvmField.flatMap(MemberFeatureCallParser::getFeatureInformation)
                            .map(
                                    featureInformation ->
                                            new MemberFeatureCall(featureInformation, false));
        }
        final ReferenceOperator foundOperator =
                foundOperatorOptional.orElseThrow(
                        () ->
                                new UnsupportedReferenceExpressionException(
                                        "The MemberFeatureCall couldn't be parsed"));

        return parseAndAppendFollowingExpressionOperators(strategy, expression, foundOperator);
    }

    private static Optional<FeatureInformation> getFeatureInformation(JvmField jvmField) {
        return Optional.ofNullable(jvmField)
                .map(JvmField::getType)
                .flatMap(
                        jvmTypeReference -> {
                            final JvmType jvmType = jvmTypeReference.getType();
                            return Optional.of(
                                    new FeatureInformation(
                                            jvmField.getSimpleName(),
                                            jvmType.getSimpleName(),
                                            jvmType.getIdentifier()));
                        });
    }

    private static boolean isListType(JvmTypeReference typeReference) {
        return Optional.ofNullable(typeReference)
                .map(JvmTypeReference::getType)
                .map(JvmType::getIdentifier)
                .map(LIST_IDENTIFIER::equals)
                .orElse(false);
    }

    private static Optional<FeatureInformation> getListFeatureInformation(JvmField jvmField) {
        return Optional.ofNullable(jvmField)
                .map(JvmField::getType)
                .flatMap(CastingUtils::asParameterizedTypeReference)
                .filter(JvmTypeReferenceUtils::hasExactlyOneArgument)
                .flatMap(JvmTypeReferenceUtils::getFirstArgument)
                .flatMap(CastingUtils::asParameterizedTypeReference)
                .map(
                        field ->
                                new FeatureInformation(
                                        jvmField.getSimpleName(),
                                        field.getType().getSimpleName(),
                                        field.getType().getIdentifier()));
    }
}
