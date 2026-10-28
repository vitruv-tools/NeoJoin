package tools.vitruv.optggs.transpiler;

import tools.vitruv.neojoin.expression_parser.model.FeatureCall;
import tools.vitruv.neojoin.expression_parser.model.FeatureInformation;
import tools.vitruv.neojoin.expression_parser.model.FlatMap;
import tools.vitruv.neojoin.expression_parser.model.Map;
import tools.vitruv.neojoin.expression_parser.model.FindAny;
import tools.vitruv.neojoin.expression_parser.model.MemberFeatureCall;
import tools.vitruv.neojoin.expression_parser.model.ReferenceFilter;
import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.model.predicate_expression.ComparisonOperator;
import tools.vitruv.neojoin.expression_parser.model.predicate_expression.ConstantValue;
import tools.vitruv.optggs.operators.*;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.filters.ConstantFilter;
import tools.vitruv.optggs.operators.filters.FunctionFilter;
import tools.vitruv.optggs.operators.projections.DerivedProjection;
import tools.vitruv.optggs.operators.projections.SimpleProjection;
import tools.vitruv.optggs.operators.reference_operator.ReferenceOperatorChain;
import tools.vitruv.optggs.operators.selection.*;
import tools.vitruv.optggs.transpiler.operators.*;
import tools.vitruv.optggs.transpiler.operators.filters.ResolvedConstantFilter;
import tools.vitruv.optggs.transpiler.operators.filters.ResolvedFunctionFilter;
import tools.vitruv.optggs.transpiler.operators.patterns.*;
import tools.vitruv.optggs.transpiler.operators.projections.ResolvedDerivedProjection;
import tools.vitruv.optggs.transpiler.operators.projections.ResolvedSimpleProjection;
import tools.vitruv.optggs.transpiler.operators.reference_operators.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TranspilerQueryResolver extends QueryResolver<ResolvedView, ResolvedQuery, ResolvedSelection, ResolvedProjection, ResolvedReferenceOperatorChain, ResolvedFilter, ResolvedContainment, ResolvedLink, ResolvedPattern, ResolvedPatternLink> {

    @Override
    ResolvedView createView(List<ResolvedQuery> queries) {
        return new ResolvedView(queries);
    }

    @Override
    public ResolvedQuery resolveQuery(Query query, Optional<ResolvedContainment> containment, List<ResolvedLink> links) {
        var selection = resolveSelection(query.selection());
        var projections = query.projections().stream().map(this::resolveProjection).toList();
        var referenceOperators = query.referenceOperators().stream().map(this::resolveReferenceOperatorChain).toList();
        var filters = query.filters().stream().map(this::resolveFilter).toList();
        return new ResolvedQuery(selection, projections, referenceOperators, filters, containment, links);
    }

    @Override
    public ResolvedSelection resolveSelection(Selection selection) {
        return new ResolvedSelection(resolvePattern(selection.source()), resolvePattern(selection.target()));
    }

    @Override
    public ResolvedProjection resolveProjection(Projection projection) {
        return switch (projection) {
            case SimpleProjection(var source, var target, var sourceProperty, var targetProperty) ->
                new ResolvedSimpleProjection(resolvePattern(source), target, sourceProperty, targetProperty);
            case DerivedProjection derivedProjection -> new ResolvedDerivedProjection(derivedProjection);
            default -> throw new RuntimeException("Unknown projection type while resolving");
        };
    }

    @Override
    ResolvedReferenceOperatorChain resolveReferenceOperatorChain(
            ReferenceOperatorChain referenceOperator) {
        final List<ResolvedReferenceOperator> referenceOperatorChain = new ArrayList<>();
        final FQN emptySourceFqn = new FQN(referenceOperator.sourceNamespace(), null);

        ReferenceOperator currentReferenceOperator = referenceOperator.referenceOperator();
        while (currentReferenceOperator != null) {
            final ResolvedReferenceOperator resolvedOperator;
            switch (currentReferenceOperator) {
                case FeatureCall featureCall ->
                        resolvedOperator =
                                new ResolvedFeatureCall(
                                        emptySourceFqn.withLocalName(featureCall.getSimpleName()));
                case MemberFeatureCall memberFeatureCall when !memberFeatureCall.isCollection() -> {
                    final FeatureInformation featureInformation =
                            memberFeatureCall.getFeatureInformation();
                    resolvedOperator =
                            new ResolvedSingleMemberFeatureCall(
                                    featureInformation.getFeatureName(),
                                    emptySourceFqn.withLocalName(
                                            featureInformation.getFeatureClassSimpleName()));
                }
                case MemberFeatureCall memberFeatureCall when memberFeatureCall.isCollection() -> {
                    final FeatureInformation featureInformation =
                            memberFeatureCall.getFeatureInformation();
                    resolvedOperator =
                            new ResolvedCollectionMemberFeatureCall(
                                    featureInformation.getFeatureName(),
                                    emptySourceFqn.withLocalName(
                                            featureInformation.getFeatureClassSimpleName()));
                }
                case Map map -> {
                    final FeatureInformation featureInformation = map.getFeatureInformation();
                    resolvedOperator =
                            new ResolvedMap(
                                    featureInformation.getFeatureName(),
                                    emptySourceFqn.withLocalName(
                                            featureInformation.getFeatureClassSimpleName()));
                }
                case FlatMap flatMap -> {
                    final FeatureInformation featureInformation = flatMap.getFeatureInformation();
                    resolvedOperator =
                            new ResolvedFlatMap(
                                    featureInformation.getFeatureName(),
                                    emptySourceFqn.withLocalName(
                                            featureInformation.getFeatureClassSimpleName()));
                }
                case ReferenceFilter filter -> {
                    resolvedOperator =
                            new ResolvedReferenceFilter(
                                    filter.getFeature(),
                                    toLogicOperator(filter.getOperator()),
                                    toConstantExpression(filter.getConstantValue()));
                }
                case FindAny ignored -> resolvedOperator = new ResolvedFindAny();
                default -> throw new IllegalStateException("Unsupported reference operator chain");
            }
            referenceOperatorChain.add(resolvedOperator);

            currentReferenceOperator = currentReferenceOperator.getFollowingOperator();
        }

        // Add a collector at the end of the reference chain
        referenceOperatorChain.add(
                new ResolvedCollectReferences(
                        referenceOperator.targetRoot(),
                        referenceOperator.targetLeaf(),
                        referenceOperator.targetField()));
        return new ResolvedReferenceOperatorChain(
                referenceOperatorChain, referenceOperator.targetRoot());
    }

    @Override
    public ResolvedFilter resolveFilter(Filter filter) {
        return switch (filter) {
            case ConstantFilter constantFilter -> new ResolvedConstantFilter(constantFilter);
            case FunctionFilter functionFilter -> new ResolvedFunctionFilter(functionFilter);
            default -> throw new RuntimeException("Unknown filter type while resolving");
        };
    }

    @Override
    public ResolvedContainment createContainment(ResolvedPattern source, ResolvedPattern target, List<ResolvedFilter> filters) {
        return new ResolvedContainment(source, target, filters);
    }

    @Override
    public ResolvedLink createLink(ResolvedPattern source, ResolvedPattern target, List<ResolvedFilter> filters) {
        return new ResolvedLink(source, target, filters);
    }

    @Override
    ResolvedPattern createPattern(List<ResolvedPatternLink> links) {
        return new ResolvedPattern(links);
    }

    @Override
    ResolvedPatternLink resolvePatternLink(PatternLink patternLink) {
        return switch (patternLink) {
            case From form -> new ResolvedFrom(form.element());
            case Join join -> new ResolvedJoin(join.element(), join.constrainedProperties());
            case ThetaJoin thetaJoin -> new ResolvedThetaJoin(thetaJoin.element(), thetaJoin.function());
            case Ref ref -> new ResolvedRef(ref.element(), ref.reference());
            default -> throw new RuntimeException("Unknown pattern type while resolving");
        };
    }

    private static LogicOperator toLogicOperator(ComparisonOperator comparisonOperator) {
        return switch (comparisonOperator) {
            case Equals -> LogicOperator.Equals;
            case NotEquals -> LogicOperator.NotEquals;
            case LessThan -> LogicOperator.LessThan;
            case LessEquals -> LogicOperator.LessEquals;
            case GreaterThan -> LogicOperator.MoreThan;
            case GreaterEquals -> LogicOperator.MoreEquals;
        };
    }

    private static ConstantExpression toConstantExpression(ConstantValue value) {
        if (value.isString()) {
            return ConstantExpression.String(value.getValue());
        }

        return new ConstantExpression(value.getValue());
    }
}
