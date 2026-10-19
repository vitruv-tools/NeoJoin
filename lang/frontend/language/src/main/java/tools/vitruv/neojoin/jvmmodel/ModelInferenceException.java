package tools.vitruv.neojoin.jvmmodel;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;

/**
 * ModelInferenceException models errors that can occur during JVM model inference.
 **/
public abstract sealed class ModelInferenceException extends Exception {

    protected ModelInferenceException(String message) {
        super(message);
    }

    public static final class UnknownAttributeType extends ModelInferenceException {

        public UnknownAttributeType(EDataType attributeType, String packageUri) {
            super("unknown reference type '%s' in %s".formatted(attributeType, packageUri));
        }
    }

    public static final class AttributeTypeMissesInstanceClass extends ModelInferenceException {

        public AttributeTypeMissesInstanceClass(EDataType attributeType, String packageUri) {
            super("unexpected attribute type without instance class '%s' in %s".formatted(attributeType, packageUri));
        }
    }

    public static final class UnknownReferenceType extends ModelInferenceException {

		public UnknownReferenceType(EClass attributeType, String packageUri) {
            super("unknown attribute type '%s' in %s".formatted(attributeType, packageUri));
		}
    }

}
