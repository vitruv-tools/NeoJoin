package tools.vitruv.neojoin.transformation;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.neojoin.aqr.AQRTargetClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Mapping from source instances to target instances. Given that one source class can be the source for multiple
 * target classes, this mapping does also depend on the target class. When {@link #get(EObject, AQRTargetClass, boolean) queried}
 * for a target instance this class checks that there is exactly one such instance and throws otherwise.
 */
public class TargetMap {

    private sealed interface Mapping {

        Mapping join(Mapping other);

    }

    private record None() implements Mapping {

        @Override
        public Mapping join(Mapping other) {
            return other;
        }

    }

    private record One(EObject value) implements Mapping {

        @Override
        public Mapping join(Mapping other) {
            return switch (other)  {
                case None ignored -> this;
                default -> new Many();
            };
        }

    }

    private record Many() implements Mapping {

        @Override
        public Mapping join(Mapping other) {
            return this;
        }

    }

    /**
     * Mappings grouped by source instance, so that lookups only scan the mappings of one source instance.
     */
    private final Map<EObject, Map<AQRTargetClass, Mapping>> map = new HashMap<>();

    private Mapping getMapping(EObject source, AQRTargetClass targetClass) {
        return map.getOrDefault(source, Map.of()).getOrDefault(targetClass, new None());
    }

    private Mapping getMappingInstanceOf(EObject source, AQRTargetClass targetClass) {
        return map.getOrDefault(source, Map.of()).entrySet().stream()
            .filter(entry -> entry.getKey().equals(targetClass) || entry.getKey().allSuperClasses().contains(targetClass))
            .map(Entry::getValue)
            .reduce(new None(), Mapping::join);
    }

    /**
     * Register a new mapping from the source instance to the target instance via the target class.
     */
    public void register(EObject source, AQRTargetClass targetClass, EObject target) {
        var previous = getMapping(source, targetClass);
        map.computeIfAbsent(source, k -> new HashMap<>())
            .put(targetClass, previous.join(new One(target)));
    }

    /**
     * Retrieve the target instance that is mapped to the given source instance and target class or subclasses or the target class.
     *
     * @throws TransformatorException if none or multiple target instances are mapped to the given source instance and target class
     */
    public EObject get(EObject source, AQRTargetClass targetClass, boolean allowSubclasses) {
        Mapping mapping;
        if (allowSubclasses) {
            mapping = getMappingInstanceOf(source, targetClass);
        } else {
            mapping = getMapping(source, targetClass);
        }

        return switch (mapping) {
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
