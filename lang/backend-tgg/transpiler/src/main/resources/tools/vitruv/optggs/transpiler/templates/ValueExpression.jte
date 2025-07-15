@import tools.vitruv.optggs.operators.expressions.ValueExpression
@import tools.vitruv.optggs.operators.expressions.VariableExpression
@import tools.vitruv.optggs.operators.expressions.ConstantExpression
@param ValueExpression expression
@if(expression instanceof ConstantExpression constant)${constant.value()}
@elseif(expression instanceof VariableExpression variable)<${variable.name()}>
@endif