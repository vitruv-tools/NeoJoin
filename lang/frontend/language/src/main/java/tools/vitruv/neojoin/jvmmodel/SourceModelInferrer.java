package tools.vitruv.neojoin.jvmmodel;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.utils.EMFUtils;
import tools.vitruv.neojoin.utils.EValueHolder;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static tools.vitruv.neojoin.utils.Assertions.check;
import static tools.vitruv.neojoin.utils.Assertions.fail;

/**
 * Generate a jvm model for all available source meta-models to allow Xbase expressions to reference their attributes.
 */
public class SourceModelInferrer {

    /**
     * Used to store the represented source {@link EClass} within a generated {@link JvmType}.
     */
    private static class EcoreSourceHolder extends EValueHolder<EClassifier> {

        public EcoreSourceHolder(EClassifier value) {
            super(value);
        }

        public static void set(EObject object, EClassifier value) {
            install(object, new SourceModelInferrer.EcoreSourceHolder(value));
        }

        public static @Nullable EClassifier getOrNull(EObject object) {
            return retrieveOrNull(object, SourceModelInferrer.EcoreSourceHolder.class);
        }

    }

    /**
     * Retrieve the source {@link EClass} that the given {@link JvmType} represents.
     *
     * @param type jvm type
     * @return represented {@link EClass} or {@code null}
     */
    public static @Nullable EClassifier getEClassifierOrNull(JvmType type) {
        return EcoreSourceHolder.getOrNull(type);
    }

    private final TypeRegistry typeRegistry;

    private final Set<EPackage> packages;
    private final JvmTypeReferenceBuilder references;

    public SourceModelInferrer(
        TypeRegistry typeRegistry,
        EPackage.Registry packageRegistry,
        JvmTypeReferenceBuilder references
    ) {
        this.packages = EMFUtils.collectAvailablePackages(packageRegistry);
        this.typeRegistry = typeRegistry;
        this.references = references;
    }

    public void infer() {
        check(typeRegistry.isEmpty(), "type registry is not empty");

        for (var pack : packages) {
            forEachEnumIn(pack, this::createEnum);
            forEachClassIn(pack, this::createClass);
        }

        for (var pack : packages) {
            forEachClassIn(pack, this::populateClass);
        }
    }

    private void forEachClassIn(EPackage pack, Consumer<EClass> consumer) {
        for (var classifier : pack.getEClassifiers()) {
            if (classifier instanceof EClass eClazz) {
                consumer.accept(eClazz);
            }
        }
    }

    private void forEachEnumIn(EPackage pack, Consumer<EEnum> consumer) {
        for (var classifier : pack.getEClassifiers()) {
            if (classifier instanceof EEnum eEnum) {
                consumer.accept(eEnum);
            }
        }
    }

    private void createEnum(EEnum eEnum) {
        var type = TypesFactory.eINSTANCE.createJvmEnumerationType();
        type.setSimpleName(eEnum.getName());
        // package URI serves as a unique identifier to prevent name collisions
        type.setPackageName(eEnum.getEPackage().getNsURI());
        type.setVisibility(JvmVisibility.PUBLIC);

        for (var literal : eEnum.getELiterals()) {
            var enumLiteral = TypesFactory.eINSTANCE.createJvmEnumerationLiteral();
            enumLiteral.setSimpleName(literal.getName());
            type.getMembers().add(enumLiteral);
        }

        EcoreSourceHolder.set(type, eEnum);
        typeRegistry.addEnum(eEnum, type);
    }

    private void createClass(EClass eClazz) {
        var type = TypesFactory.eINSTANCE.createJvmGenericType();
        type.setSimpleName(eClazz.getName());
        // package URI serves as a unique identifier to prevent name collisions
        type.setPackageName(eClazz.getEPackage().getNsURI());
        type.setVisibility(JvmVisibility.PUBLIC);

        EcoreSourceHolder.set(type, eClazz);
        typeRegistry.addClass(eClazz, type);
    }

    private void populateClass(EClass eClazz) {
        var type = typeRegistry.getClass(eClazz);
        check(type != null);

        /* copy all features into the class even inherited ones because keeping the
         * original type hierarchy is irrelevant for code completion and validation */
        for (var feature : eClazz.getEAllStructuralFeatures()) {
            var field = TypesFactory.eINSTANCE.createJvmField();
            field.setSimpleName(feature.getName());
            field.setType(getFeatureType(feature));
            field.setVisibility(JvmVisibility.PUBLIC);
            field.setFinal(true);

            type.getMembers().add(field);
        }
    }

    private JvmTypeReference getFeatureType(EStructuralFeature feature) {
        JvmTypeReference type;
        if (feature instanceof EAttribute attr) {
            type = getAttributeType(attr);
        } else if (feature instanceof EReference ref) {
            type = getReferenceType(ref);
        } else {
            type = fail("unexpected feature: " + feature);
        }

        if (feature.getUpperBound() == 0 || feature.getUpperBound() == 1) {
            return type;
        } else {
            return references.typeRef(List.class, type);
        }
    }

    private JvmTypeReference getAttributeType(EAttribute attr) {
        if (attr.getEAttributeType() instanceof EEnum eEnum) {
            var enumType = typeRegistry.getEnum(eEnum);
            check(
                enumType != null,
                () -> "unknown attribute type '%s' in %s".formatted(attr.getEAttributeType(), getPackageUri(attr))
            );
            return references.typeRef(enumType);
        } else {
            var type = attr.getEAttributeType().getInstanceClass();
            check(
                type != null,
                () -> "unexpected attribute type without instance class '%s' in %s".formatted(
                    attr.getEAttributeType(),
                    getPackageUri(attr)
                )
            );
            return references.typeRef(type);
        }
    }

    private JvmTypeReference getReferenceType(EReference ref) {
        var type = typeRegistry.getClass(ref.getEReferenceType());
        check(
            type != null,
            () -> "unknown reference type '%s' in %s".formatted(ref.getEReferenceType(), getPackageUri(ref))
        );
        return references.typeRef(type);
    }

    private static String getPackageUri(EStructuralFeature feature) {
        return EMFUtils.getRootPackage(feature.getEContainingClass().getEPackage()).getNsURI();
    }

}
