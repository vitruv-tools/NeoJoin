package tools.vitruv.neojoin.tgg.emsl_utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;

import java.net.URL;

public abstract class AbstractEmslGeneratorTest {
    private static final String ECORE_METAMODEL_FILENAME = "SimpleCarMetamodel.ecore";

    public ResourceSet getMetamodelResourceSet() {
        final ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet
                .getResourceFactoryRegistry()
                .getExtensionToFactoryMap()
                .put("ecore", new EcoreResourceFactoryImpl());

        final URL resourceUrl = getClass().getClassLoader().getResource(ECORE_METAMODEL_FILENAME);
        assertThat(resourceUrl).isNotNull();
        final URI ecoreResourceURI = URI.createURI(resourceUrl.toString());

        final Resource ecoreResource = resourceSet.getResource(ecoreResourceURI, true);
        assertThat(ecoreResource).isNotNull();
        assertThat(ecoreResource.getContents()).isNotEmpty();

        return resourceSet;
    }
}
