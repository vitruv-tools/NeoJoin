package tools.vitruv.neojoin.collector;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.common.EMFUtils;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import static tools.vitruv.neojoin.common.Assertions.fail;

/**
 * Finds the collected meta-models that the source model inference cannot map to Java types. It infers the types of
 * all collected meta-models, not only of the imported ones (see {@code SourceModelInferrer}), so one such meta-model
 * would break every query.
 */
final class UnmappableMetaModels {

    private UnmappableMetaModels() {}

    /**
     * Removes the meta-models that the source model inference cannot map from the registry, and the meta-models that
     * reference a removed or an already ignored one.
     *
     * @param collected      the packages of the registry in the order in which they were collected
     * @param alreadyIgnored packages that are not in the registry for another reason (reported elsewhere)
     * @return an issue for each removed meta-model, in the order of {@code collected}
     */
    static List<CollectorIssue> remove(
        List<EPackage> collected,
        EPackage.Registry registry,
        Set<EPackage> alreadyIgnored
    ) {
        var ignored = findIgnoredMetaModels(collected, registry, alreadyIgnored);
        var issues = collected.stream()
            .filter(ignored::contains)
            .map(pack -> createIgnoreIssue(pack, registry, ignored))
            .toList();
        ignored.forEach(pack -> registry.remove(pack.getNsURI()));
        return issues;
    }

    /**
     * Returns the meta-models that are not {@link #isMappable mappable} and, transitively, the meta-models that
     * reference one of them.
     */
    private static Set<EPackage> findIgnoredMetaModels(
        List<EPackage> collected,
        EPackage.Registry registry,
        Set<EPackage> alreadyIgnored
    ) {
        Set<EPackage> ignored = new HashSet<>(alreadyIgnored);
        Queue<EPackage> unprocessed = new ArrayDeque<>(alreadyIgnored);
        for (var pack : collected) {
            if (!isMappable(pack, registry)) {
                ignored.add(pack);
                unprocessed.add(pack);
            }
        }
        while (!unprocessed.isEmpty()) {
            var ignoredPackage = unprocessed.remove();
            for (var pack : collected) {
                if (!ignored.contains(pack)
                    && referencedTypes(pack).anyMatch(type -> type.getEPackage() == ignoredPackage)) {
                    ignored.add(pack);
                    unprocessed.add(pack);
                }
            }
        }
        return ignored;
    }

    private static CollectorIssue createIgnoreIssue(EPackage pack, EPackage.Registry registry, Set<EPackage> ignored) {
        var untyped = findFeatureWithoutType(pack);
        if (untyped.isPresent()) {
            var feature = untyped.get();
            return new CollectorIssue.FeatureWithoutType(
                pack.getNsURI(),
                feature.getEContainingClass().getName() + "." + feature.getName()
            );
        }
        // a type of another ignored meta-model is not supported either; the issue names that meta-model instead
        var unsupported = referencedTypes(pack)
            .filter(type -> !isSupported(type, registry) && !isOtherIgnored(type.getEPackage(), pack, ignored))
            .findFirst();
        if (unsupported.isPresent()) {
            var type = unsupported.get();
            if (isDataTypeWithoutInstanceClass(type)) {
                return new CollectorIssue.DataTypeWithoutInstanceClass(pack.getNsURI(), formatUri(type));
            }

            var globalPackage = findGlobalPackageReplacingCopy(type, registry);
            if (globalPackage != null) {
                // formatUri would give the namespace URI of the global package, which the meta-model can use instead
                return new CollectorIssue.ReferenceToReplacedCopy(
                    pack.getNsURI(),
                    EcoreUtil.getURI(type).toString(),
                    globalPackage.getNsURI()
                );
            }
            return new CollectorIssue.ReferenceOutsideModelPath(pack.getNsURI(), formatUri(type));
        }

        var ignoredPackage = referencedTypes(pack)
            .map(EClassifier::getEPackage)
            .filter(referenced -> isOtherIgnored(referenced, pack, ignored))
            .findFirst()
            .orElseGet(() -> fail("ignored meta-model without a reason: " + pack.getNsURI()));
        return new CollectorIssue.ReferenceToIgnoredMetaModel(pack.getNsURI(), ignoredPackage.getNsURI());
    }

    /**
     * Returns the global package that the registry contains instead of the package of the type, if the type belongs to
     * a copy of a global package (loaded from a file through a relative reference, see
     * {@link CollectorIssue.GlobalPackageConflict}).
     */
    private static @Nullable EPackage findGlobalPackageReplacingCopy(EClassifier type, EPackage.Registry registry) {
        // an unresolved proxy or a class outside any package
        if (type.getEPackage() == null) {
            return null;
        }

        // the type is not supported, so its own package is not in the registry
        var namespaceUri = type.getEPackage().getNsURI();
        var globalPackage = EPackage.Registry.INSTANCE.getEPackage(namespaceUri);
        return globalPackage != null && registry.getEPackage(namespaceUri) == globalPackage ? globalPackage : null;
    }

    private static boolean isOtherIgnored(@Nullable EPackage referenced, EPackage pack, Set<EPackage> ignored) {
        return referenced != pack && ignored.contains(referenced);
    }

    /**
     * Returns whether the source model inference can map the classes of the package to Java types: every feature has
     * a type ({@code SourceModelInferrer.getFeatureType} would fail with a {@link NullPointerException}), and every
     * super class and feature type is {@link #isSupported supported}.
     */
    private static boolean isMappable(EPackage pack, EPackage.Registry registry) {
        return findFeatureWithoutType(pack).isEmpty()
            && referencedTypes(pack).allMatch(type -> isSupported(type, registry));
    }

    private static Optional<EStructuralFeature> findFeatureWithoutType(EPackage pack) {
        return classes(pack)
            .flatMap(clazz -> clazz.getEStructuralFeatures().stream())
            .filter(feature -> feature.getEType() == null)
            .findFirst();
    }

    /**
     * Returns the super classes and feature types of the classes in the given package.
     */
    private static Stream<EClassifier> referencedTypes(EPackage pack) {
        return classes(pack).flatMap(EMFUtils::getReferencedTypes);
    }

    private static Stream<EClass> classes(EPackage pack) {
        return pack.getEClassifiers().stream()
            .filter(EClass.class::isInstance)
            .map(EClass.class::cast);
    }

    /**
     * Returns whether the source model inference can map the type: it is resolved, belongs to a package of the
     * registry or to Ecore ({@code SourceModelInferrer.getReferenceType} and {@code getAttributeType} only know the
     * classes and enumerations of these packages), and, if it is a data type, has an instance class
     * ({@code getAttributeType} maps data types to their instance classes).
     */
    private static boolean isSupported(EClassifier type, EPackage.Registry registry) {
        if (type.eIsProxy() || isDataTypeWithoutInstanceClass(type)) {
            return false;
        }
        var pack = type.getEPackage();
        return pack == EcorePackage.eINSTANCE || pack != null && registry.get(pack.getNsURI()) == pack;
    }

    private static boolean isDataTypeWithoutInstanceClass(EClassifier type) {
        return !type.eIsProxy()
            && type instanceof EDataType
            && !(type instanceof EEnum)
            && type.getInstanceClass() == null;
    }

    /**
     * Returns the URI by which meta-models reference the type: the namespace URI of its root package and its
     * fragment, also when it was loaded from a file outside the meta-model path.
     */
    private static String formatUri(EClassifier type) {
        if (type.eIsProxy() || type.getEPackage() == null) {
            return EcoreUtil.getURI(type).toString();
        }
        var rootPackage = EMFUtils.getRootPackage(type.getEPackage());
        return rootPackage.getNsURI() + "#" + EcoreUtil.getURI(type).fragment();
    }
}
