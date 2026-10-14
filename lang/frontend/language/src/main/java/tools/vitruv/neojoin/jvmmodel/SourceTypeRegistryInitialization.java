package tools.vitruv.neojoin.jvmmodel;

import com.google.inject.ImplementedBy;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;

/**
 * Initializes a type registry to contain the types of the view source metamodels.
 */
@ImplementedBy(InferringSourceTypeRegistryInitialization.class)
public interface SourceTypeRegistryInitialization {
    void initialize(TypeRegistry typeRegistry, EPackage.Registry packageRegistry, JvmTypeReferenceBuilder typeReferenceBuilder, ResourceSet resourceSet);
}
