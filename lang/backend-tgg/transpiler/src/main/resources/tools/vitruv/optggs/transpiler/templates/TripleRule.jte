@import gg.jte.support.ForSupport
@import tools.vitruv.optggs.transpiler.NameResolver
@import tools.vitruv.optggs.transpiler.tgg.AttributeConstraint
@import tools.vitruv.optggs.transpiler.tgg.Correspondence
@import tools.vitruv.optggs.transpiler.tgg.TripleRule
@import tools.vitruv.optggs.transpiler.tgg.Node
@param TripleRule rule
@param String schema
@param NameResolver nameResolver
tripleRule ${nameResolver.resolveRuleName(rule)} : ${schema} {
    source {
    @for(Node node: rule.allSourcesAsSlice().nodes())@template.Node(node = node, nameResolver = nameResolver)
    @endfor
    }
    target {
    @for(Node node: rule.allTargetsAsSlice().nodes())@template.Node(node = node, nameResolver = nameResolver)
    @endfor
    }
    correspondence {
    @for(Correspondence corr: rule.correspondences())@if(corr.isGreen())++@endif ${corr.source().id()} <- :${nameResolver.resolveCorrespondenceName(corr)} -> ${corr.target().id()}
    @endfor
    }
    attributeConstraints {
    @for(AttributeConstraint constraint: rule.constraints())${constraint.name()}(
        @for(var param: ForSupport.of(constraint.parameters())) ${param.get().attribute()} = @template.ValueExpression(expression=param.get().value()) @if(!param.isLast()),@endif@endfor
        )
    @endfor
    }
}
