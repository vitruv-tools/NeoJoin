package tools.vitruv.neojoin.transformation.emf;

import org.eclipse.emf.ecore.EObject;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.common.ClassifierNames;
import tools.vitruv.neojoin.common.Pair;
import tools.vitruv.neojoin.model.AQRImport;
import tools.vitruv.neojoin.model.AQRTargetClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapping from source instances to target instances and their target classes. Given that one source class can be the
 * source for multiple target classes, a lookup can be restricted to a target class (including its sub classes). When
 * {@link #get queried} for a target instance this class checks that there is exactly one such instance and throws
 * otherwise.
 */
public class TargetMap {

    private final List<AQRImport> imports;
    private final Map<EObject, List<Pair<EObject, AQRTargetClass>>> targetsBySource = new HashMap<>();

    /**
     * Creates an empty mapping.
     *
     * @param imports the imports of the query, used to write source classes in error messages as in the query
     */
    public TargetMap(List<AQRImport> imports) {
        this.imports = List.copyOf(imports);
    }

    /**
     * Register a new mapping from the source instance to the target instance via the target class.
     */
    public void register(EObject source, AQRTargetClass targetClass, EObject target) {
        targetsBySource.computeIfAbsent(source, k -> new ArrayList<>()).add(new Pair<>(target, targetClass));
    }

    /**
     * Retrieve the target instance that is mapped to the given source instance and the target class or one of its
     * sub classes.
     *
     * @throws TransformatorException if none or multiple target instances are mapped to the given source instance
     *                                and target class
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
            .filter(target -> targetClass == null || isSameOrSubClass(target.right(), targetClass))
            .toList();
        var classInfo = targetClass != null ? " of class '%s'".formatted(targetClass.name()) : "";

        if (targets.isEmpty()) {
            throw new TransformatorException(
                "no target instance%s found for source instance of class %s".formatted(
                    classInfo, writeSourceClass(source)
                )
            );
        } else if (targets.size() > 1) {
            throw new TransformatorException(
                "multiple target instances%s found for source instance of class %s".formatted(
                    classInfo, writeSourceClass(source)
                )
            );
        }
        return targets.get(0).left();
    }

    /**
     * Returns the quoted name of the class of the source instance as the query writes it. The names are only
     * computed for an error message.
     */
    private String writeSourceClass(EObject source) {
        var aliasByPackage = imports.stream()
            .collect(Collectors.toMap(AQRImport::pack, AQRImport::alias, (first, second) -> first));
        var names = new ClassifierNames(aliasByPackage, aliasByPackage.keySet());
        return names.write(source.eClass()).quoted();
    }

    private static boolean isSameOrSubClass(AQRTargetClass targetClass, AQRTargetClass superClass) {
        return targetClass.equals(superClass) || targetClass.allSuperClasses().contains(superClass);
    }

}
