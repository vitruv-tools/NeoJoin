@import tools.vitruv.optggs.driver.ConstraintSolver
@import tools.vitruv.optggs.driver.Project
@param Project p

@for(ConstraintSolver solver : p.constraintSolvers())
attributeConstraint ${solver.name()}(${solver.parameters().stream().map(param -> param + ": " + solver.parameterType(param)).collect(Collectors.joining(", "))}) {
    ${String.join(", ", solver.supportedBindings())}
    gen: ${String.join(", ", solver.supportedGenBindings())}
}
@endfor
