package tools.vitruv.neojoin.jvmmodel;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.common.types.JvmEnumerationType;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static tools.vitruv.neojoin.utils.Assertions.check;

/**
 * A registry that stores a mapping from Ecore source classes and enums to the generated corresponding JVM types.
 */
public class TypeRegistry extends ResourceImpl {

	private final Map<EClass, JvmGenericType> classes = new HashMap<>();
	private final Map<EEnum, JvmEnumerationType> enums = new HashMap<>();

	public TypeRegistry(URI uri) {
		super(uri);
	}

	public boolean isEmpty() {
		return classes.isEmpty() && enums.isEmpty();
	}

	public @Nullable JvmGenericType getClass(EClass clazz) {
		return classes.get(clazz);
	}

	public void addClass(EClass clazz, JvmGenericType type) {
		var previous = classes.put(clazz, type);
		check(previous == null, () -> "Type already registered for class: " + clazz);
		getContents().add(type);
	}

	public @Nullable JvmEnumerationType getEnum(EEnum eEnum) {
		return enums.get(eEnum);
	}

	public void addEnum(EEnum eEnum, JvmEnumerationType type) {
		var previous = enums.put(eEnum, type);
		check(previous == null, () -> "Type already registered for enum: " + eEnum);
		getContents().add(type);
	}

}
