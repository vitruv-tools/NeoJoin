package tools.vitruv.neojoin.transformation.predicates;

import org.eclipse.emf.ecore.EObject;

import java.util.function.Predicate;

public class AnyPredicate implements Predicate<EObject> {

    @Override
    public boolean test(EObject o) {
        return true;
    }

    @Override
    public String toString() {
        return "true";
    }

}
