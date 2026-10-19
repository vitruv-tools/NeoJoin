@import tools.vitruv.optggs.driver.ConstraintSolver
@import tools.vitruv.optggs.driver.Project
@param Project p

@for(ConstraintSolver solver : p.constraintSolvers())
attributeConstraint ${solver.name()}(${String.join(", ", solver.parameters().stream().map(param -> param + ": " + solver.parameterType(param)).toList())}) {
    ${String.join(", ", solver.supportedBindings())}
    @if(!solver.supportedGenBindings().isEmpty())
    gen: ${String.join(", ", solver.supportedGenBindings())}
    @endif
}
@endfor