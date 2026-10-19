package tools.vitruv.neojoin.jvmmodel;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;

public class InferringSourceTypeRegistryInitialization implements SourceTypeRegistryInitialization {
    @Override
    public void initialize(TypeRegistry typeRegistry, EPackage.Registry packageRegistry, JvmTypeReferenceBuilder typeReferenceBuilder, ResourceSet resourceSet) throws ModelInferenceException {
        new SourceModelInferrer(typeRegistry, packageRegistry, typeReferenceBuilder).infer();
    }
}
