package tools.vitruv.neojoin.transformation;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.neojoin.aqr.AQRTargetClass;
import tools.vitruv.neojoin.utils.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapping from source instances to target instances. Given that one source class can be the source for multiple
 * target classes, this mapping does also depend on the target class. When {@link #get(EObject, AQRTargetClass) queried}
 * for a target instance this class checks that there is exactly one such instance and throws otherwise.
 */
public class TargetMap {

    private sealed interface Mapping {

        Mapping with(EObject value);

    }

    private record None() implements Mapping {

        @Override
        public Mapping with(EObject value) {
            return new One(value);
        }

    }

    private record One(EObject value) implements Mapping {

        @Override
        public Mapping with(EObject value) {
            return new Many();
        }

    }

    private record Many() implements Mapping {

        @Override
        public Mapping with(EObject value) {
            return this;
        }

    }

    private final Map<Pair<EObject, AQRTargetClass>, Mapping> map = new HashMap<>();

    private Mapping getMapping(EObject source, AQRTargetClass targetClass) {
        return map.computeIfAbsent(
            new Pair<>(source, targetClass),
            k -> new None()
        );
    }

    /**
     * Register a new mapping from the source instance to the target instance via the target class.
     */
    public void register(EObject source, AQRTargetClass targetClass, EObject target) {
        var previous = getMapping(source, targetClass);
        map.put(
            new Pair<>(source, targetClass),
            previous.with(target)
        );
    }

    /**
     * Retrieve the target instance that is mapped to the given source instance and target class.
     *
     * @throws TransformatorException if none or multiple target instances are mapped to the given source instance and target class
     */
    public EObject get(EObject source, AQRTargetClass targetClass) {
        return switch (getMapping(source, targetClass)) {
            case One(var value) -> value;
            case None ignored -> throw new TransformatorException(
                "no target instance of class '%s' found for source instance of class '%s'".formatted(
                    targetClass.name(), source.eClass().getName()
                )
            );
            case Many ignored -> throw new TransformatorException(
                "multiple target instances of class '%s' found for source instance of class '%s'".formatted(
                    targetClass.name(), source.eClass().getName()
                )
            );
        };
    }

}
