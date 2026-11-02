@import tools.vitruv.optggs.transpiler.NameResolver
@import tools.vitruv.optggs.transpiler.graph.pattern.GraphPattern
@import tools.vitruv.optggs.transpiler.graph.tgg.CorrespondenceType
@import tools.vitruv.optggs.transpiler.graph.tgg.TripleGrammar
@import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule
@param TripleGrammar grammar
@param NameResolver nameResolver
tripleGrammar ${grammar.getName()} {
    source {
        @for(String s : grammar.getSourceMetamodels())
        ${s}
@endfor
    }
    target {
        @for(String s : grammar.getTargetMetamodels())
    ${s}
@endfor
    }
    correspondence {
        @for(CorrespondenceType c : grammar.getCorrespondenceTypes())
        ${nameResolver.resolveName(c.source())} <- ${nameResolver.resolveCorrespondenceName(c)} -> ${nameResolver.resolveName(c.target())}
    @endfor
    }
    rules {
        @for(TripleRule r : grammar.getRules())
        ${nameResolver.resolveRuleName(r)}
@endfor
    }
    constraints {
        @for(GraphPattern constraint : grammar.getConstraints())
            ${nameResolver.resolveConstraintName(constraint)}
        @endfor
    }
}