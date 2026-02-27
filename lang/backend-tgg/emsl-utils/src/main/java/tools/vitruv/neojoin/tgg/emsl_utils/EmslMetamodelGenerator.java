package tools.vitruv.neojoin.tgg.emsl_utils;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.FileOutput;

import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class EmslMetamodelGenerator {
    private static final String EMSL_METAMODEL_COLLECTION_TEMPLATE = "EmslMetamodelCollection.jte";

    public static void generateMetamodels(ResourceSet resourceSet, Path output) {
        TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Plain);
        try (FileOutput out = new FileOutput(output)) {
            engine.render(
                    EMSL_METAMODEL_COLLECTION_TEMPLATE,
                    Map.of("rs", resourceSet, "metaModelEnum", new HashMap<>()),
                    out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
