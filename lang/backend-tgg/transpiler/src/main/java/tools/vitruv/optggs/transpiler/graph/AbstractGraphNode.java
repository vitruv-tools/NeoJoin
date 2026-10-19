package tools.vitruv.optggs.transpiler.graph;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.NonFinal;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.expressions.VariableExpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;

@Value
@NonFinal
public abstract class AbstractGraphNode<
        L extends AbstractGraphLink<SELF>, SELF extends AbstractGraphNode<L, SELF>> {
    protected String id;
    protected FQN type;
    protected NameRepository nameRepository;

    @Getter(AccessLevel.NONE)
    protected ArrayList<L> links;

    @Getter(AccessLevel.NONE)
    protected LinkedHashSet<Attribute> attributes;

    public abstract SELF deepCopy(GraphNodeDeepCopyHelper<SELF> copyHelper);

    public VariableExpression addVariableAttribute(String name, LogicOperator operator) {
        var variableName = getVariableNameForProperty(name);
        return addVariableAttribute(name, operator, new VariableExpression(variableName));
    }

    public VariableExpression addVariableAttribute(
            String name, LogicOperator operator, VariableExpression variable) {
        addAttribute(new Attribute(name, operator, variable));
        return variable;
    }

    public void addConstantAttribute(
            String name, LogicOperator operator, ConstantExpression value) {
        addAttribute(new Attribute(name, operator, value));
    }

    public void addAttribute(Attribute attribute) {
        attributes.add(attribute);
    }

    public Collection<Attribute> attributes() {
        return Collections.unmodifiableCollection(attributes);
    }

    public void removeAttribute(Attribute attribute) {
        attributes.remove(attribute);
    }

    private Optional<Attribute> getFirstAttributeByName(String name) {
        return attributes.stream().filter(attribute -> attribute.name().equals(name)).findFirst();
    }

    /**
     * Returns a variable name for the provided property.
     *
     * <p>If a variable already exists, the existing variable name is returned. Otherwise, a new
     * variable name is generated.
     */
    private String getVariableNameForProperty(String propertyName) {
        Optional<Attribute> existingAttributeOptional = this.getFirstAttributeByName(propertyName);
        if (existingAttributeOptional.isEmpty()) {
            return nameRepository.getLower(propertyName);
        }

        final Attribute existingAttribute = existingAttributeOptional.get();
        if (existingAttribute.value() instanceof VariableExpression variableExpression) {
            return variableExpression.name();
        }

        return nameRepository.getLower(propertyName);
    }

    public Collection<L> links() {
        return Collections.unmodifiableCollection(links);
    }

    public SELF getFirstLinkTarget(String link) {
        return this.links.stream()
                .filter(someLink -> someLink.getName().equals(link))
                .findFirst()
                .orElseThrow()
                .getTarget();
    }

    public void addLink(L link) {
        this.links.add(link);
    }

    @Override
    public String toString() {
        var links = String.join(",", links().stream().map(Objects::toString).toList());
        var attributes = String.join(",", attributes().stream().map(Objects::toString).toList());
        return "<" + id + ": " + type.fqn() + ";" + links + ";" + attributes + ">";
    }
}
