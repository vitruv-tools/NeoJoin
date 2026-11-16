package tools.vitruv.neojoin.ide.custom;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmEnumerationType;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.jvmmodel.TypeRegistry;

/**
 * Finds the Ecore element of a source meta-model that an element of a query refers to.
 */
final class SourceModelElements {

    private SourceModelElements() {
    }

    /**
     * Returns the Ecore element for the target of a reference in a query, or {@code null} if the target is not part of
     * a source meta-model (for example a Java type or an element of the query).
     * <p>
     * Classes, enums, data types, packages and features that a query names directly are Ecore elements already (only
     * named elements count: an annotation has no name to locate). The types inferred for expressions
     * ({@code r.name} in {@code from Restaurant r}) are mapped back with the {@link TypeRegistry} that created them.
     */
    static @Nullable ENamedElement ecoreElementOf(EObject target) {
        if (target instanceof ENamedElement element) {
            return element;
        }
        if (target instanceof JvmField field && field.eResource() instanceof TypeRegistry registry
            && field.getDeclaringType() instanceof JvmGenericType type) {
            return featureOf(registry, type, field);
        }
        if (target instanceof JvmGenericType type && type.eResource() instanceof TypeRegistry registry) {
            return registry.getClass(type);
        }
        if (target instanceof JvmEnumerationType type && type.eResource() instanceof TypeRegistry registry) {
            return registry.getEnum(type);
        }
        return null;
    }

    /**
     * The inferred class has a field for each of {@code getEAllStructuralFeatures()}, in this order (see
     * {@code SourceModelInferrer}), so the position of the field is the position of its feature. This also finds the
     * inherited feature in the file of the super class, and it does not depend on the names: a class that declares a
     * feature name again (invalid Ecore) has two fields of the same name.
     */
    private static @Nullable ENamedElement featureOf(TypeRegistry registry, JvmGenericType type, JvmField field) {
        var clazz = registry.getClass(type);
        int index = type.getMembers().indexOf(field);
        if (clazz == null || index >= clazz.getEAllStructuralFeatures().size()) {
            return null;
        }
        var feature = clazz.getEAllStructuralFeatures().get(index);
        // safeguard: a different order of the members would point to the wrong feature
        return feature.getName().equals(field.getSimpleName()) ? feature : null;
    }

}
