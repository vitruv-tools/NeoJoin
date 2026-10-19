package tools.vitruv.optggs.transpiler.operators.projections;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.transpiler.operators.ResolvedProjection;
import tools.vitruv.optggs.transpiler.operators.patterns.ResolvedPattern;
import tools.vitruv.optggs.transpiler.graph.Attribute;
import tools.vitruv.optggs.transpiler.graph.tgg.TripleRule;

public class ResolvedSimpleProjection implements ResolvedProjection {
    private final ResolvedPattern source;
    private final FQN target;
    private final String sourceProperty;
    private final String targetProperty;

    public ResolvedSimpleProjection(
            ResolvedPattern source, FQN target, String sourceProperty, String targetProperty) {
        this.source = source;
        this.target = target;
        this.sourceProperty = sourceProperty;
        this.targetProperty = targetProperty;
    }

    @Override
    public void extendRule(TripleRule rule) {
        var slice = rule.allSourcesAsSlice();
        source.extendSlice(slice);
        var bottomNode = slice.findByType(source.bottom()).orElseThrow();
        var targetNode =
                rule.allTargetsAsSlice()
                        .findByType(target)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Projection: could not find "
                                                        + target.fqn()
                                                        + " in target graph"));
        var variable = bottomNode.addVariableAttribute(sourceProperty, LogicOperator.Equals);
        targetNode.addAttribute(new Attribute(targetProperty, LogicOperator.Equals, variable));
    }

    @Override
    public String toString() {
        return "Π("
                + source
                + "::"
                + sourceProperty
                + " => "
                + target.fqn()
                + "::"
                + targetProperty
                + ")";
    }
}
