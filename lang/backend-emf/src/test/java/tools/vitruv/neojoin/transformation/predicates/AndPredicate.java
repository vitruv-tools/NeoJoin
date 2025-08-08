package tools.vitruv.neojoin.transformation.predicates;

import org.eclipse.emf.ecore.EObject;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AndPredicate implements Predicate<EObject> {

    private final List<Predicate<EObject>> predicates;

    public AndPredicate(List<Predicate<EObject>> predicates) {
        this.predicates = predicates;
    }

    @Override
    public boolean test(EObject o) {
        return predicates.stream().allMatch(p -> p.test(o));
    }

    @Override
    public String toString() {
        return predicates.stream()
            .map(Object::toString)
            .collect(Collectors.joining(" && ", "( ", " )"));
    }

}
