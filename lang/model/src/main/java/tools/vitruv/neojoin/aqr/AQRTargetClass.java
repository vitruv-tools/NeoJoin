package tools.vitruv.neojoin.aqr;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Class within the target model.
 *
 * @implNote This class is not a record because it requires identity and a record's
 * {@link Record#equals(Object) equals()} and {@link Record#hashCode() hashCode()}
 * explicitly forbid it.
 * Identity is required on a conceptional level, because a target class should not be equal to
 * any other target class even if all properties are equal (e.g. by creating an exact copy of a query).
 * Additionally, it helps to prevent errors with self references in the {@link #features() features}
 * that would lead to endless recursion in {@link #equals(Object) equals()} and {@link #hashCode()}.
 */
@SuppressWarnings("ClassCanBeRecord")
public final class AQRTargetClass {

    private final String name;
    private final boolean isAbstract;
    private final @Nullable AQRSource source;
    private final List<AQRTargetClass> superClasses;
    private final List<AQRFeature> features;

    /**
     * @param name     name of the target class
     * @param source   source of the target class
     * @param features features of the target class
     */
    public AQRTargetClass(
        String name,
        boolean isAbstract,
        @Nullable AQRSource source,
        List<AQRTargetClass> superClasses,
        List<AQRFeature> features
    ) {
        this.name = name;
        this.isAbstract = isAbstract;
        this.source = source;
        this.superClasses = superClasses;
        this.features = features;
    }

    public String name() {
        return name;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public @Nullable AQRSource source() {
        return source;
    }

    public List<AQRTargetClass> superClasses() {
        return superClasses;
    }

    public List<AQRTargetClass> allSuperClasses() {
        // BFS
        Queue<AQRTargetClass> queue = new LinkedList<>(superClasses());
        Set<AQRTargetClass> allSuperClasses = new HashSet<>();

        while (!queue.isEmpty()) {
            var superClass = queue.poll();
            allSuperClasses.add(superClass);

            var newSuperClasses = superClass.superClasses();
            newSuperClasses.removeAll(allSuperClasses);
            queue.addAll(newSuperClasses);
        }

        return allSuperClasses.stream().toList();
    }

    public List<AQRFeature> features() {
        return features;
    }

    @Override
    public String toString() {
        return "TargetClass[name=%s, source=%s, superClasses=%s, features=%s]".formatted(
            name,
            source,
            superClasses,
            features
        );
    }

}
