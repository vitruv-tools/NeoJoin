package tools.vitruv.neojoin.transformation;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.transformation.predicates.InstancePredicates;

import java.util.List;
import java.util.Map;

import static tools.vitruv.neojoin.transformation.InstanceModelAssertions.assertThat;

/**
 * Expressions over a source model of generated EMF classes (Ecore itself) instead of dynamic objects.
 */
public class GeneratedModelTest extends AbstractTransformationTest implements InstancePredicates {

    @Override
    protected List<String> getMetaModelPaths() {
        return List.of();
    }

    @Override
    protected List<String> getInstanceModelPaths() {
        return List.of();
    }

    @Override
    protected EPackage.Registry createPackageRegistry() {
        var registry = new EPackageRegistryImpl();
        registry.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
        return registry;
    }

    @Override
    protected Map<EPackage, Resource> createInstanceModelRegistry() {
        var pack = EcoreFactory.eINSTANCE.createEPackage();
        pack.setName("shop");
        for (var name : List.of("Item", "Customer")) {
            var eClass = EcoreFactory.eINSTANCE.createEClass();
            eClass.setName(name);
            pack.getEClassifiers().add(eClass);
        }

        var resource = new ResourceSetImpl().createResource(URI.createURI("shop.ecore"));
        resource.getContents().add(pack);
        return Map.of(EcorePackage.eINSTANCE, resource);
    }

    @Test
    void expressionReadsFeature() {
        var result = internalTransform("""
            export package to "http://example.com"

            import "http://www.eclipse.org/emf/2002/Ecore" as emf

            from EClass c where c.name.length > 4 create {
                c.name
                upper := c.name.toUpperCase
            }
            """);

        assertThat(result)
            .hasInstance("EClass", named("Customer"), c -> assertThat(c).hasAttribute("upper", "CUSTOMER"))
            .hasNoMoreInstances();
    }

}
