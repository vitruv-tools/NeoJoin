package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.utils;

import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.Optional;
import java.util.stream.Stream;

public class JvmTypeReferenceUtils {
    private JvmTypeReferenceUtils() {}

    public static boolean hasExactlyOneArgument(JvmParameterizedTypeReference typeReference) {
        return Optional.ofNullable(typeReference)
                .map(JvmParameterizedTypeReference::getArguments)
                .map(args -> args.size() == 1)
                .orElse(false);
    }

    public static Optional<JvmTypeReference> getFirstArgument(
            JvmParameterizedTypeReference typeReference) {
        return Optional.ofNullable(typeReference)
                .map(JvmParameterizedTypeReference::getArguments)
                .map(EList::stream)
                .flatMap(Stream::findFirst);
    }
}
