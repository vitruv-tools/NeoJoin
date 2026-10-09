package tools.vitruv.neojoin.jvmmodel;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.common.types.JvmEnumerationType;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.jspecify.annotations.Nullable;

import static tools.vitruv.neojoin.utils.Assertions.check;

/**
 * A registry that stores a mapping from Ecore source classes and enums to the generated corresponding JVM types.
 */
public class TypeRegistry extends ResourceImpl {
    private final BiMap<EClass, JvmGenericType> classes = HashBiMap.create();
    private final BiMap<EEnum, JvmEnumerationType> enums = HashBiMap.create();

    public TypeRegistry(URI uri) {
        super(uri);
    }

    public boolean isEmpty() {
        return classes.isEmpty() && enums.isEmpty();
    }

    public @Nullable JvmGenericType getClass(EClass clazz) {
        return classes.get(clazz);
    }

    public @Nullable EClass getClass(JvmGenericType genericType) {
        return classes.inverse().get(genericType);
    }

    public void addClass(EClass clazz, JvmGenericType type) {
        registerClass(clazz, type);
        getContents().add(type);
    }

    public void referenceClass(EClass clazz, JvmGenericType type) {
        registerClass(clazz, type);
    }

    private void registerClass(EClass clazz, JvmGenericType type) {
        check(!classes.containsKey(clazz), () -> "Type already registered for class: " + clazz);
        check(!classes.containsValue(type), () -> "Class already registered for type: " + type);
        classes.put(clazz, type);
    }

    public @Nullable JvmEnumerationType getEnum(EEnum eEnum) {
        return enums.get(eEnum);
    }

    public @Nullable EEnum getEnum(JvmEnumerationType type) {
        return enums.inverse().get(type);
    }

    public void addEnum(EEnum eEnum, JvmEnumerationType type) {
        registerEnum(eEnum, type);
        getContents().add(type);
    }

    public void referenceEnum(EEnum eEnum, JvmEnumerationType type) {
        registerEnum(eEnum, type);
    }

    private void registerEnum(EEnum eEnum, JvmEnumerationType type) {
        check(!enums.containsKey(eEnum), () -> "Type already registered for enum: " + eEnum);
        check(!enums.containsValue(type), () -> "Enum already registered for type: " + type);
        enums.put(eEnum, type);
    }
}
