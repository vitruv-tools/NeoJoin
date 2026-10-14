package tools.vitruv.neojoin.jvmmodel;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;

public class InferringSourceTypeRegistryInitialization implements SourceTypeRegistryInitialization {
    @Override
    public void initialize(TypeRegistry typeRegistry, EPackage.Registry packageRegistry, JvmTypeReferenceBuilder typeReferenceBuilder, ResourceSet resourceSet) {
        new SourceModelInferrer(typeRegistry, packageRegistry, typeReferenceBuilder).infer();
    }
}
