package tools.vitruv.optggs.operators;

import java.util.Objects;

public class FQN {
    private final String metamodelName;
    private final String localName;

    public FQN(String metamodelName, String localName) {
        this.metamodelName = metamodelName;
        this.localName = localName;
    }

    public FQN(String localName) {
        this("", localName);
    }

    public String metamodelName() {
        return metamodelName;
    }

    public String localName() {
        return localName;
    }

    public String fqn() {
        return metamodelName + "." + localName;
    }

    public FQN resolveInMetamodel(String metamodel) {
        return new FQN(metamodel, localName);
    }

    public FQN withLocalName(String localName) {
        return new FQN(metamodelName, localName);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FQN fqn)) return false;
        return Objects.equals(metamodelName, fqn.metamodelName)
                && Objects.equals(localName, fqn.localName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metamodelName, localName);
    }
}
