@import tools.vitruv.optggs.transpiler.NameResolver
@import tools.vitruv.optggs.transpiler.graph.pattern.GraphPattern
@import tools.vitruv.optggs.transpiler.graph.pattern.PatternNode
@param GraphPattern constraint
@param NameResolver nameResolver
constraint ${nameResolver.resolveConstraintName(constraint)} = forbid ${nameResolver.resolvePatternName(constraint)}
pattern ${nameResolver.resolvePatternName(constraint)} {
    @for(PatternNode node: constraint.getNodes())@template.pattern.PatternNode(node = node, nameResolver = nameResolver)
    @endfor
}
