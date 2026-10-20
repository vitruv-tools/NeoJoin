package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils;

import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

import java.util.Optional;
import java.util.stream.Stream;

public class JvmMemberCallUtils {
    private JvmMemberCallUtils() {}

    public static boolean hasExactlyOneMemberCallArgument(XMemberFeatureCall featureCall) {
        return Optional.ofNullable(featureCall)
                .map(XMemberFeatureCall::getMemberCallArguments)
                .map(args -> args.size() == 1)
                .orElse(false);
    }

    public static Optional<XExpression> getFirstArgument(XMemberFeatureCall featureCall) {
        return Optional.ofNullable(featureCall)
                .map(XMemberFeatureCall::getMemberCallArguments)
                .map(EList::stream)
                .flatMap(Stream::findFirst);
    }
}
