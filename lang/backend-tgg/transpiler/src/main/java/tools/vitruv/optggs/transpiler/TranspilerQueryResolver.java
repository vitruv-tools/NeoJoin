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
        if (projection instanceof SimpleProjection sp) {
            return new ResolvedSimpleProjection(resolvePattern(sp.source()), sp.target(), sp.sourceProperty(), sp.targetProperty());
        } else if (projection instanceof DerivedProjection dp) {
            return new ResolvedDerivedProjection(dp);
        } else {
            throw new RuntimeException("Unknown projection type while resolving");
        }
    }

    @Override
    public ResolvedFilter resolveFilter(Filter filter) {
        if (filter instanceof ConstantFilter cf) {
            return new ResolvedConstantFilter(cf);
        } else if (filter instanceof FunctionFilter ff) {
            return new ResolvedFunctionFilter(ff);
        } else {
            throw new RuntimeException("Unknown filter type while resolving");
        }
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
        if (patternLink instanceof From f) {
            return new ResolvedFrom(f.element());
        } else if (patternLink instanceof Join j) {
            return new ResolvedJoin(j.element(), j.constrainedProperties());
        } else if (patternLink instanceof ThetaJoin tj) {
            return new ResolvedThetaJoin(tj.element(), tj.function());
        } else if (patternLink instanceof Ref r) {
            return new ResolvedRef(r.element(), r.reference());
        } else {
            throw new RuntimeException("Unknown pattern type while resolving");
        }
    }
}
