package tools.vitruv.neojoin.jvmmodel;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.util.function.Consumer;

public class SourceModel {
    private static final URI SourceTypeRegistryURI = URI.createURI("SourceTypeRegistry");

    private SourceModel() {

    }

    public static TypeRegistry getOrCreateSourceTypeRegistry(ResourceSet resourceSet, Consumer<TypeRegistry> initializer) {
        var typeRegistry = (TypeRegistry) resourceSet.getResource(SourceTypeRegistryURI, false);
        if (typeRegistry == null) {
            typeRegistry = new TypeRegistry(SourceTypeRegistryURI);
            initializer.accept(typeRegistry);
            resourceSet.getResources().add(typeRegistry);
        }
        return typeRegistry;
    }

    public static TypeRegistry getSourceTypeRegistry(ResourceSet resourceSet) {
        return (TypeRegistry) resourceSet.getResource(SourceTypeRegistryURI, false);
    }
}
