package tools.vitruv.optggs.transpiler;

import tools.vitruv.optggs.operators.*;
import tools.vitruv.optggs.operators.filters.ConstantFilter;
import tools.vitruv.optggs.operators.filters.FunctionFilter;
import tools.vitruv.optggs.operators.projections.DerivedProjection;
import tools.vitruv.optggs.operators.projections.SimpleProjection;
import tools.vitruv.optggs.operators.selection.*;
import tools.vitruv.optggs.transpiler.operators.*;
import tools.vitruv.optggs.transpiler.operators.filters.ResolvedConstantFilter;
import tools.vitruv.optggs.transpiler.operators.filters.ResolvedFunctionFilter;
import tools.vitruv.optggs.transpiler.operators.patterns.*;
import tools.vitruv.optggs.transpiler.operators.projections.ResolvedDerivedProjection;
import tools.vitruv.optggs.transpiler.operators.projections.ResolvedSimpleProjection;

import java.util.List;
import java.util.Optional;

public class TranspilerQueryResolver extends QueryResolver<ResolvedView, ResolvedQuery, ResolvedSelection, ResolvedProjection, ResolvedFilter, ResolvedContainment, ResolvedLink, ResolvedPattern, ResolvedPatternLink> {

    @Override
    ResolvedView createView(List<ResolvedQuery> queries) {
        return new ResolvedView(queries);
    }

    @Override
    public ResolvedQuery resolveQuery(Query query, Optional<ResolvedContainment> containment, List<ResolvedLink> links) {
        var selection = resolveSelection(query.selection());
        var projections = query.projections().stream().map(this::resolveProjection).toList();
        var filters = query.filters().stream().map(this::resolveFilter).toList();
        return new ResolvedQuery(selection, projections, filters, containment, links);
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
            case null, default -> throw new RuntimeException("Unknown projection type while resolving");
        };
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
}
