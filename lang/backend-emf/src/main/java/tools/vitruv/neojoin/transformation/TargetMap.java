package tools.vitruv.neojoin.transformation;

import org.eclipse.emf.ecore.EObject;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.aqr.AQRTargetClass;
import tools.vitruv.neojoin.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapping from source instances to target instances and their target classes. Given that one source class can be the
 * source for multiple target classes, a lookup can be restricted to a target class. When {@link #get queried} for a
 * target instance this class checks that there is exactly one such instance and throws otherwise.
 */
public class TargetMap {

    private final Map<EObject, List<Pair<EObject, AQRTargetClass>>> targetsBySource = new HashMap<>();

    /**
     * Register a new mapping from the source instance to the target instance via the target class.
     */
    public void register(EObject source, AQRTargetClass targetClass, EObject target) {
        targetsBySource.computeIfAbsent(source, k -> new ArrayList<>()).add(new Pair<>(target, targetClass));
    }

    /**
     * Retrieve the target instance that is mapped to the given source instance and target class.
     *
     * @throws TransformatorException if none or multiple target instances are mapped to the given source instance and target class
     */
    public EObject get(EObject source, AQRTargetClass targetClass) {
        return getUnique(source, targetClass);
    }

    /**
     * Retrieve the target instance that is mapped to the given source instance regardless of the target class. Used
     * for references to Ecore classes, which have no target class.
     *
     * @throws TransformatorException if none or multiple target instances are mapped to the given source instance
     */
    public EObject get(EObject source) {
        return getUnique(source, null);
    }

    private EObject getUnique(EObject source, @Nullable AQRTargetClass targetClass) {
        var targets = targetsBySource.getOrDefault(source, List.of()).stream()
            .filter(target -> targetClass == null || target.right() == targetClass)
            .toList();
        var classInfo = targetClass != null ? " of class '%s'".formatted(targetClass.name()) : "";

        if (targets.isEmpty()) {
            throw new TransformatorException(
                "no target instance%s found for source instance of class '%s'".formatted(
                    classInfo, source.eClass().getName()
                )
            );
        } else if (targets.size() > 1) {
            throw new TransformatorException(
                "multiple target instances%s found for source instance of class '%s'".formatted(
                    classInfo, source.eClass().getName()
                )
            );
        }
        return targets.get(0).left();
    }

}
