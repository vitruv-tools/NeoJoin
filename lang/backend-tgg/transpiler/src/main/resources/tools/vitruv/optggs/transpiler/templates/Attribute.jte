@import tools.vitruv.optggs.transpiler.tgg.Attribute
@import tools.vitruv.optggs.transpiler.tgg.Node
@param Node node
@param Attribute attribute
.${attribute.name()} @template.LogicOperator(operator = attribute.operator(), node = node)       @template.ValueExpression(expression = attribute.value())