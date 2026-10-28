@import tools.vitruv.optggs.transpiler.NameResolver
@import tools.vitruv.optggs.transpiler.graph.pattern.GraphPattern
@import tools.vitruv.optggs.transpiler.graph.tgg.TripleGrammar
@import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule
@param TripleGrammar grammar
@param NameResolver nameResolver
import "platform:/resource/${grammar.getName()}/src/SourceMetamodel.msl"
import "platform:/resource/${grammar.getName()}/src/TargetMetamodel.msl"
import "platform:/resource/${grammar.getName()}/src/AttributeConstraints.msl"
@template.Schema(grammar = grammar, nameResolver = nameResolver)
@for(TripleRule rule : grammar.getRules())@template.tgg.TripleRule(rule = rule, schema = grammar.getName(), nameResolver = nameResolver)
@endfor
@for(GraphPattern constraint : grammar.getConstraints())@template.pattern.ConstraintPattern(constraint = constraint, nameResolver = nameResolver)
@endfor
