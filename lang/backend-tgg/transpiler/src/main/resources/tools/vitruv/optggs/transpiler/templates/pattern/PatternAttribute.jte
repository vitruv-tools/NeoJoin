@import tools.vitruv.optggs.transpiler.graph.Attribute
@import tools.vitruv.optggs.transpiler.graph.pattern.PatternNode
@param PatternNode node
@param Attribute attribute
.${attribute.name()} @template.pattern.PatternLogicOperator(operator = attribute.operator(), node = node) @template.ValueExpression(expression = attribute.value())
