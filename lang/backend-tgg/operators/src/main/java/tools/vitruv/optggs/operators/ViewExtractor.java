package tools.vitruv.optggs.operators;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.jspecify.annotations.NonNull;

import tools.vitruv.neojoin.aqr.*;
import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.PatternMatchingStrategy;
import tools.vitruv.optggs.operators.exception.UnsupportedProjectionException;
import tools.vitruv.optggs.operators.selection.Pattern;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ViewExtractor {
    public static View viewFromAQR(
            AQR aqr, @NonNull PatternMatchingStrategy patternMatchingStrategy)
            throws UnsupportedReferenceExpressionException {
        final View view = new View();

        // Add queries for all classes except the Root class
        for (final AQRTargetClass targetClass : aqr.classesWithoutRoot()) {
            view.addQuery(
                    queryFromTargetClass(
                            aqr.export().name(), targetClass, patternMatchingStrategy));
        }

        return view;
    }

    private static Query queryFromTargetClass(
            String targetNamespace,
            AQRTargetClass targetClass,
            @NonNull PatternMatchingStrategy patternMatchingStrategy)
            throws UnsupportedReferenceExpressionException {
        final FQN source =
                Optional.ofNullable(targetClass.source())
                        .map(AQRSource::from)
                        .map(ViewExtractor::fqn)
                        .orElse(fqn(targetClass.name()));
        final FQN targetRoot = new FQN(targetNamespace, targetClass.name());

        final Map<String, FQN> namedRefs = new HashMap<>();
        namedRefs.put("", source);
        namedRefs.put("self", source);

        Pattern sourcePattern = Pattern.from(source);
        final List<AQRJoin> joins =
                Optional.ofNullable(targetClass.source()).map(AQRSource::joins).orElse(List.of());
        for (final AQRJoin join : joins) {
            sourcePattern = joinFromAST(join, sourcePattern, namedRefs);
        }
        final Query query = new Query(new Selection(sourcePattern, Pattern.from(targetRoot)));

        if (targetClass.source() != null && targetClass.source().condition() != null) {
            throw new UnsupportedOperationException("Condition expressions are not supported");
        }

        applyProjectionsFromAST(
                targetClass.features(), query, namedRefs, targetRoot, patternMatchingStrategy);

        if (targetClass.source() != null && !targetClass.source().groupingExpressions().isEmpty()) {
            throw new UnsupportedOperationException("Grouping expressions are not supported");
        }

        return query;
    }

    private static Pattern joinFromAST(AQRJoin join, Pattern pattern, Map<String, FQN> namedRefs) {
        final FQN fqn = fqn(join.from());
        Optional<String> joinFromAlias =
                Optional.ofNullable(join.from().alias()).filter(alias -> !alias.isEmpty());
        joinFromAlias.ifPresent(s -> namedRefs.put(s, fqn));

        if (join.type() == AQRJoin.Type.Inner) {
            var attributes =
                    join.featureConditions().stream()
                            .map(AQRJoin.FeatureCondition::features)
                            .flatMap(List::stream)
                            .map(attr -> new Tuple<>(attr, attr))
                            .toList();
            return pattern.join(fqn, attributes);
        }
        // TODO: Other join types

        return pattern;
    }

    private static void applyProjectionsFromAST(
            List<AQRFeature> features,
            Query query,
            Map<String, FQN> namedRefs,
            FQN targetRoot,
            @NonNull PatternMatchingStrategy patternMatchingStrategy)
            throws UnsupportedReferenceExpressionException {
        for (AQRFeature feature : features) {
            switch (feature) {
                case AQRFeature.Attribute attribute
                        when attribute.kind() instanceof AQRFeature.Kind.Copy copy -> {
                    final String sourceAttributeName = copy.source().getName();
                    final String targetAttributeName = attribute.name();
                    query.project(sourceAttributeName, targetAttributeName);
                }
                case AQRFeature.Attribute attribute
                        when attribute.kind() instanceof AQRFeature.Kind.Calculate ->
                        throw new UnsupportedProjectionException(
                                String.format(
                                        "Calculate expressions are not supported: %s", feature));
                case AQRFeature.Reference reference
                        when reference.kind() instanceof AQRFeature.Kind.Copy copy ->
                        // We could "mock" a ReferenceOperator FeatureCall + MemberFeatureCall here
                        // to support this
                        throw new UnsupportedProjectionException(
                                String.format("Copying of References not supported: %s", feature));
                case AQRFeature.Reference reference
                        when reference.kind()
                                instanceof AQRFeature.Kind.Calculate calculateReference -> {
                    final ReferenceOperator operator =
                            patternMatchingStrategy.parseReferenceOperator(
                                    calculateReference.expression());
                    final String sourceTypeNamespace =
                            Optional.ofNullable(reference.type().source())
                                    .map(AQRSource::from)
                                    .map(AQRFrom::clazz)
                                    .map(EClass::getEPackage)
                                    .map(EPackage::getName)
                                    .orElse(null);
                    final FQN targetLeaf = targetRoot.withLocalName(reference.type().name());
                    query.referenceOperator(
                            sourceTypeNamespace,
                            targetRoot,
                            targetLeaf,
                            reference.name(),
                            operator);
                }
                case null, default -> throw new UnsupportedProjectionException(feature);
            }
        }
    }

    private static FQN fqn(AQRFrom from) {
        return new FQN(from.clazz().getEPackage().getName(), from.clazz().getName());
    }

    private static FQN fqn(String name) {
        if (name.contains(".")) {
            final String[] splitted = name.split("\\.");
            return new FQN(splitted[0], splitted[1]);
        } else {
            return new FQN(name);
        }
    }
}
