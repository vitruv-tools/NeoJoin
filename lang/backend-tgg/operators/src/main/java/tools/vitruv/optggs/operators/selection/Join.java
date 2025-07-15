package tools.vitruv.optggs.operators.selection;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.Tuple;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class Join implements PatternLink {
    private final FQN element;
    private final List<Tuple<String, String>> constrainedProperties;

    public Join(FQN element, Collection<Tuple<String, String>> constrainedProperties) {
        this.element = element;
        this.constrainedProperties = List.copyOf(constrainedProperties);
    }

    public Join(FQN element, String originProperty, String destinationProperty) {
        this.element = element;
        this.constrainedProperties = List.of(new Tuple<>(originProperty, destinationProperty));
    }

    public Join(FQN element, String property) {
        this.element = element;
        this.constrainedProperties = List.of(new Tuple<>(property, property));
    }

    @Override
    public FQN element() {
        return element;
    }

    public List<Tuple<String, String>> constrainedProperties() {
        return constrainedProperties;
    }

    @Override
    public String toString() {
        var conditions = String.join(",", constrainedProperties.stream().map(props -> props.first() + "==" + props.last()).toList());
        return " ⨝(" + conditions + ") " + element.fqn();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Join join)) return false;
        return Objects.equals(element, join.element) && Objects.equals(constrainedProperties, join.constrainedProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, constrainedProperties);
    }
}
