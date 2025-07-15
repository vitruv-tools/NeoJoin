package tools.vitruv.optggs.transpiler.tgg;

import tools.vitruv.optggs.operators.FQN;
import tools.vitruv.optggs.operators.LogicOperator;
import tools.vitruv.optggs.operators.expressions.ConstantExpression;
import tools.vitruv.optggs.operators.expressions.ValueExpression;
import tools.vitruv.optggs.operators.expressions.VariableExpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

public class Node {
    private final String id;
    private final FQN type;
    private boolean green;
    private final NameRepository nameRepository;
    private final HashMap<String, Property> properties = new HashMap<>();
    private final HashMap<String, Link> links = new HashMap<>();
    private final HashMap<String, Attribute> attributes = new HashMap<>();

    private Node(String id, FQN type, boolean green, NameRepository nameRepository) {
        this.id = id;
        this.type = type;
        this.green = green;
        this.nameRepository = nameRepository;
    }

    public static Node Black(String id, FQN type, NameRepository nameRepository) {
        return new Node(id, type, false, nameRepository);
    }

    public static Node Green(String id, FQN type, NameRepository nameRepository) {
        return new Node(id, type, true, nameRepository);
    }

    public String id() {
        return id;
    }

    public FQN type() {
        return type;
    }

    public boolean isGreen() {
        return green;
    }

    public Node makeGreen() {
        this.green = true;
        return this;
    }

    public Node makeBlack() {
        this.green = false;
        return this;
    }

    public Collection<Property> properties() {
        return properties.values();
    }

    public Property property(String name) {
        return properties.get(name);
    }

    public ValueExpression addVariableAttribute(String name, LogicOperator operator) {
        var variableName = variableNameForProperty(name);
        return addVariableAttribute(name, operator, new VariableExpression(variableName));
    }

    public ValueExpression addVariableAttribute(String name, LogicOperator operator, VariableExpression variable) {
        var existingAttribute = this.attribute(name);
        if (existingAttribute != null) {
            if (existingAttribute.operator() == operator && existingAttribute.value() instanceof VariableExpression v) {
                // keep variable attribute if the same attribute already exists
                return v;
            } else if (existingAttribute.operator() == LogicOperator.Equals && operator == LogicOperator.Equals && existingAttribute.value() instanceof ConstantExpression c) {
                return c;
            }
            throw new RuntimeException("Tried to set attribute " + name + " on " + this.id + " twice (variable)");
        }
        addAttribute(new Attribute(name, operator, variable));
        return variable;
    }

    public void addConstantAttribute(String name, LogicOperator operator, ConstantExpression value) {
        var existingAttribute = this.attribute(name);
        if (existingAttribute != null) {
            throw new RuntimeException("Tried to set attribute " + name + " on " + this.id + " twice (constant)");
        }
        addAttribute(new Attribute(name, operator, value));
    }

    public void addAttribute(Attribute attribute) {
        attributes.put(attribute.name(), attribute);
    }

    public Attribute attribute(String name) {
        return attributes.get(name);
    }

    public Collection<Attribute> attributes() {
        return attributes.values();
    }

    private String variableNameForProperty(String propertyName) {
        var existingProperty = this.property(propertyName);
        if (existingProperty instanceof VariableProperty) {
            return existingProperty.value();
        } else {
            return nameRepository.getLower(propertyName);
        }
    }

    public Collection<Link> links() {
        return this.links.values();
    }

    public void addLink(Link link) {
        this.links.put(link.name(), link);
    }

    @Override
    public String toString() {
        var links = String.join(",", links().stream().map(Objects::toString).toList());
        var attrbutes = String.join(",", attributes().stream().map(Objects::toString).toList());
        return "<" + (green ? "++" : "") + id + ": " + type.fqn() + ";" + links + ";" + attrbutes + ">";
    }
}
