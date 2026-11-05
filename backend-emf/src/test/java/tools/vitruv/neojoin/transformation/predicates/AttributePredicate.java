package tools.vitruv.neojoin.transformation.predicates;

import org.eclipse.emf.ecore.EObject;

import java.util.Objects;
import java.util.function.Predicate;

public class AttributePredicate implements Predicate<EObject> {

    private final String name;
    private final Object value;

    public AttributePredicate(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public boolean test(EObject o) {
        var feature = Objects.requireNonNull(o.eClass().getEStructuralFeature(name));
        return Objects.equals(o.eGet(feature), value);
    }

    @Override
    public String toString() {
        return "(%s == %s)".formatted(name, value);
    }

}
