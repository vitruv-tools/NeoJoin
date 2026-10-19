package tools.vitruv.optggs.transpiler.operators.patterns;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.Tuple;
import tools.vitruv.optggs.transpiler.graph.Attribute;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGSlice;
import tools.vitruv.optggs.transpiler.graph.tgg.TGGNode;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ResolvedJoin implements ResolvedPatternLink {
    private final FQN element;
    private final List<Tuple<String, String>> constrainedProperties;

    public ResolvedJoin(FQN element, Collection<Tuple<String, String>> constrainedProperties) {
        this.element = element;
        this.constrainedProperties = List.copyOf(constrainedProperties);
    }

    @Override
    public FQN element() {
        return element;
    }

    @Override
    public TGGNode extendSlice(TGGSlice slice, TGGNode lastNode) {
        TGGNode node = slice.findByType(element).orElseGet(() -> slice.addNode(element));
        for (var property : constrainedProperties) {

            var variable = lastNode.addVariableAttribute(property.first(), LogicOperator.Equals);
            node.addAttribute(new Attribute(property.last(), LogicOperator.Equals, variable));
        }
        return node;
    }

    @Override
    public String toString() {
        var conditions =
                String.join(
                        ",",
                        constrainedProperties.stream()
                                .map(props -> props.first() + "==" + props.last())
                                .toList());
        return " ⨝(" + conditions + ") " + element.fqn();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ResolvedJoin join)) return false;
        return Objects.equals(element, join.element)
                && Objects.equals(constrainedProperties, join.constrainedProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, constrainedProperties);
    }
}
