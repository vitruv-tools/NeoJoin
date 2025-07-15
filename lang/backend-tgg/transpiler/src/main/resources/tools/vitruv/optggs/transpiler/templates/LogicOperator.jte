@import tools.vitruv.optggs.operators.LogicOperator
@import tools.vitruv.optggs.transpiler.tgg.Node
@param LogicOperator operator
@param Node node
@if(operator == LogicOperator.Equals && node.isGreen()):=
@elseif(operator == LogicOperator.Equals && !node.isGreen()):
@elseif(operator == LogicOperator.NotEquals)!=
@elseif(operator == LogicOperator.LessThan)<
@elseif(operator == LogicOperator.LessEquals)<=
@elseif(operator == LogicOperator.MoreThan)>
@elseif(operator == LogicOperator.MoreEquals)>=
@endif