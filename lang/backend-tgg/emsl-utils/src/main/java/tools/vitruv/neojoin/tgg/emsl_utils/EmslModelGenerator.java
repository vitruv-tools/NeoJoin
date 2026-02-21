package tools.vitruv.neojoin.tgg.emsl_utils;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.FileOutput;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class EmslModelGenerator {
    private static final String EMSL_MODEL_COLLECTION_TEMPLATE = "EmslModelCollection.jte";

    public static void generateModels(ResourceSet resourceSet, Path output) {
        TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Plain);
        try (FileOutput out = new FileOutput(output)) {
            final Map<EObject, String> objectIDs = EmslUtils.createObjectIDs(resourceSet);
            engine.render(
                    EMSL_MODEL_COLLECTION_TEMPLATE,
                    Map.of("rs", resourceSet, "objIDs", objectIDs),
                    out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
