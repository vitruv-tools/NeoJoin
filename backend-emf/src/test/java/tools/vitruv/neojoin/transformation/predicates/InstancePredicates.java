package tools.vitruv.neojoin.transformation.predicates;

import org.eclipse.emf.ecore.EObject;

import java.util.Arrays;
import java.util.function.Predicate;

public interface InstancePredicates {

    default Predicate<EObject> attribute(String name, Object value) {
        return new AttributePredicate(name, value);
    }

    default Predicate<EObject> named(String name) {
        return new AttributePredicate("name", name);
    }

    @SuppressWarnings("unchecked")
    default Predicate<EObject> and(Predicate<EObject>... predicates) {
        return new AndPredicate(Arrays.asList(predicates));
    }

    default Predicate<EObject> any() {
        return new AnyPredicate();
    }

}
