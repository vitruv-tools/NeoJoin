@import tools.vitruv.optggs.transpiler.NameResolver
@import tools.vitruv.optggs.transpiler.tgg.TripleGrammar
@import tools.vitruv.optggs.transpiler.tgg.TripleRule
@param TripleGrammar grammar    
@param NameResolver nameResolver
import "platform:/resource/${grammar.name()}/src/SourceMetamodel.msl"
import "platform:/resource/${grammar.name()}/src/TargetMetamodel.msl"
import "platform:/resource/${grammar.name()}/src/AttributeConstraints.msl"
@template.Schema(grammar = grammar, nameResolver = nameResolver)
@for(TripleRule rule : grammar.rules())@template.TripleRule(rule = rule, schema = grammar.name(), nameResolver = nameResolver)
@endfor