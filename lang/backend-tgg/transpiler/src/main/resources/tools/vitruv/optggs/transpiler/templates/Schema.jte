@import tools.vitruv.optggs.transpiler.NameResolver
@import tools.vitruv.optggs.transpiler.tgg.CorrespondenceType
@import tools.vitruv.optggs.transpiler.tgg.TripleGrammar
@import tools.vitruv.optggs.transpiler.tgg.TripleRule
@param TripleGrammar grammar
@param NameResolver nameResolver
tripleGrammar ${grammar.name()} {
    source {
@for(String s : grammar.sourceMetamodels())
        ${s}
@endfor
    }
    target {
@for(String s : grammar.targetMetamodels())
    ${s}
@endfor
    }
    correspondence {
    @for(CorrespondenceType c : grammar.correspondenceTypes())
        ${nameResolver.resolveName(c.source())} <- ${nameResolver.resolveCorrespondenceName(c)} -> ${nameResolver.resolveName(c.target())}
    @endfor
    }
    rules {
@for(TripleRule r : grammar.rules())
        ${nameResolver.resolveRuleName(r)}
@endfor
    }
}