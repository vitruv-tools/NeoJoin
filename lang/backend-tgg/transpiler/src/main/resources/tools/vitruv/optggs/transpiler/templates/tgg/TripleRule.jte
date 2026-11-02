@import gg.jte.support.ForSupport
@import tools.vitruv.optggs.transpiler.NameResolver
@import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode
@import tools.vitruv.optggs.transpiler.graph.tgg.constraint.AttributeConstraint
@import tools.vitruv.optggs.transpiler.graph.tgg.Correspondence
@import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule
@param TripleRule rule
@param String schema
@param NameResolver nameResolver
tripleRule ${nameResolver.resolveRuleName(rule)} : ${schema} {
    source {
    @for(TGGNode node: rule.allSourcesAsSlice().nodes())@template.tgg.TGGNode(node = node, nameResolver = nameResolver)
    @endfor
    }
    target {
    @for(TGGNode node: rule.allTargetsAsSlice().nodes())@template.tgg.TGGNode(node = node, nameResolver = nameResolver)
    @endfor
    }
    correspondence {
    @for(Correspondence corr: rule.correspondences())@if(corr.isGreen())++@endif ${corr.getSource().getId()} <- :${nameResolver.resolveCorrespondenceName(corr)} -> ${corr.getTarget().getId()}
    @endfor
    }
    attributeConstraints {
    @for(AttributeConstraint constraint: rule.constraints())${constraint.getConstraintName()}(
        @for(var param: ForSupport.of(constraint.getParameters())) ${param.get().attribute()} = @template.ValueExpression(expression=param.get().value()) @if(!param.isLast()),@endif@endfor
        )
    @endfor
    }
}
