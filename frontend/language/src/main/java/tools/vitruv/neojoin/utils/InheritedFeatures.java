package tools.vitruv.neojoin.utils;

import tools.vitruv.neojoin.ast.MainQuery;
import tools.vitruv.neojoin.ast.Query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link FeatureInfo features} that the target class of a query inherits from its super classes, determined from
 * the AST. The features of a super class are its effective features (with the type and modifiers of the overridden
 * features, see {@link FeatureInfoResolver#getEffectiveFeatureInfos}).
 */
public class InheritedFeatures {

    private final Map<Query, List<FeatureInfo>> bySuperClass;

    private InheritedFeatures(Map<Query, List<FeatureInfo>> bySuperClass) {
        this.bySuperClass = bySuperClass;
    }

    /**
     * Determines the inherited features of the given query. Super classes whose features cannot be determined are
     * left out. The result is empty if the class has no super classes or if the inheritance is cyclic (which is
     * reported by a validator).
     */
    public static InheritedFeatures of(MainQuery query, FeatureInfoResolver featureInfoResolver) {
        var superClasses = AstUtils.getResolvedSuperClasses(query);
        if (superClasses.isEmpty() || AstUtils.hasCyclicSuperClasses(query)) {
            return new InheritedFeatures(Map.of());
        }

        var bySuperClass = new LinkedHashMap<Query, List<FeatureInfo>>();
        for (var superClass : superClasses) {
            var features = featureInfoResolver.getEffectiveFeatureInfos(superClass);
            if (features != null) {
                bySuperClass.put(superClass, features);
            }
        }
        return new InheritedFeatures(bySuperClass);
    }

    /**
     * Returns the features of the super classes by super class.
     */
    public Map<Query, List<FeatureInfo>> bySuperClass() {
        return Map.copyOf(bySuperClass);
    }

    /**
     * Returns the super classes that provide a feature, by name of the feature.
     */
    public Map<String, List<Query>> superClassesByFeatureName() {
        var superClassesByName = new LinkedHashMap<String, List<Query>>();
        bySuperClass.forEach((superClass, features) -> {
            features.stream()
                .map(FeatureInfo::name)
                .distinct()
                .forEach(name -> superClassesByName.computeIfAbsent(name, k -> new ArrayList<>()).add(superClass));
        });
        return superClassesByName;
    }

    /**
     * Returns whether the super classes that provide a feature with the given name do not inherit it from the same
     * declaration. Ecore does not allow two features with the same name in a class, so such a feature cannot be
     * inherited by a single class.
     */
    public boolean hasConflictingProviders(String name) {
        var superClasses = superClassesByFeatureName().getOrDefault(name, List.of());
        if (superClasses.isEmpty()) {
            return false;
        }
        var first = find(superClasses.get(0), name);
        return superClasses.stream()
            .skip(1)
            .map(superClass -> find(superClass, name))
            .anyMatch(other -> other.declaringQuery() != first.declaringQuery());
    }

    /**
     * Returns the overridden feature by name of the feature, together with the super class that provides it. If
     * several super classes inherit the feature from the same declaration, the first provider is the overridden
     * feature. Features with conflicting providers are left out.
     */
    public Map<String, Pair<Query, FeatureInfo>> overriddenFeatures() {
        var result = new LinkedHashMap<String, Pair<Query, FeatureInfo>>();
        superClassesByFeatureName().forEach((name, superClasses) -> {
            if (!hasConflictingProviders(name)) {
                var superClass = superClasses.get(0);
                result.put(name, new Pair<>(superClass, find(superClass, name)));
            }
        });
        return result;
    }

    private FeatureInfo find(Query superClass, String name) {
        return bySuperClass.get(superClass).stream().filter(feature -> feature.name().equals(name)).findFirst().orElseThrow();
    }

}
