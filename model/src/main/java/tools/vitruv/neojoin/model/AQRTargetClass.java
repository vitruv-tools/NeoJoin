package tools.vitruv.neojoin.model;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Class within the target model.
 *
 * @implNote This class is not a record because it requires identity and a record's
 *     {@link Record#equals(Object) equals()} and {@link Record#hashCode() hashCode()}
 *     explicitly forbid it.
 *     Identity is required on a conceptional level, because a target class should not be equal to
 *     any other target class even if all properties are equal (e.g. by creating an exact copy of a query).
 *     Additionally, it helps to prevent errors with self references in the {@link #features() features}
 *     that would lead to endless recursion in {@link #equals(Object) equals()} and {@link #hashCode()}.
 */
@SuppressWarnings("ClassCanBeRecord")
public final class AQRTargetClass {

    private final String name;
    private final boolean isAbstract;
    private final @Nullable AQRSource source;
    private final List<AQRTargetClass> superClasses;
    private final List<AQRFeature> features;

    /**
     * @param name         name of the target class
     * @param isAbstract   whether the target class is abstract (no instances are created for it)
     * @param source       source of the target class
     * @param superClasses direct super classes of the target class
     * @param features     features of the target class
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
        this.superClasses = new ArrayList<>(superClasses);
        this.features = new ArrayList<>(features);
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

    /**
     * @return a copy of the direct super classes, changes to the returned list do not affect this class
     */
    public List<AQRTargetClass> superClasses() {
        return List.copyOf(superClasses);
    }

    /**
     * Returns all direct and transitive super classes without duplicates (breadth first, nearest first). This is a
     * pure query: it does not modify any class and can be called repeatedly.
     *
     * @return a copy of the list of all super classes
     */
    public List<AQRTargetClass> allSuperClasses() {
        var allSuperClasses = new LinkedHashSet<AQRTargetClass>();
        var queue = new ArrayDeque<>(superClasses);

        while (!queue.isEmpty()) {
            var superClass = queue.poll();
            // only enqueue the super classes of newly found classes, so this also terminates for cyclic inheritance
            if (allSuperClasses.add(superClass)) {
                queue.addAll(superClass.superClasses);
            }
        }

        return List.copyOf(allSuperClasses);
    }

    /**
     * @return a copy of the features, changes to the returned list do not affect this class
     */
    public List<AQRFeature> features() {
        return List.copyOf(features);
    }

    /**
     * Adds a direct super class. Only used while building the AQR.
     */
    public void addSuperClass(AQRTargetClass superClass) {
        superClasses.add(superClass);
    }

    /**
     * Adds a feature. Only used while building the AQR.
     */
    public void addFeature(AQRFeature feature) {
        features.add(feature);
    }

    /**
     * Replaces all features. Only used while building the AQR.
     */
    public void replaceFeatures(List<AQRFeature> newFeatures) {
        features.clear();
        features.addAll(newFeatures);
    }

    @Override
    public String toString() {
        return "TargetClass[name=%s, source=%s, superClasses=%s, features=%s]".formatted(
            name,
            source,
            superClasses.stream().map(AQRTargetClass::name).toList(),
            features
        );
    }

}
