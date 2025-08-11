package tools.vitruv.neojoin.cli.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.vitruv.neojoin.cli.integration.Utils.compareInstanceFiles;
import static tools.vitruv.neojoin.cli.integration.Utils.getResource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import picocli.CommandLine;
import tools.vitruv.neojoin.cli.Main;

public class TransformTest {
    
    static List<String> validQueries = List.of("pizza");

    @BeforeAll
    public static void setupRegistry() {
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("xmi")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        }
    }

    /**
     * {@code java -jar cli.jar --meta-model-path=<meta-model-path> --instance-model-path=<instance-model-path> --transform=<output> <query>}
     */
    @ParameterizedTest
    @FieldSource("validQueries")
    public void testTransformInputModels(String queryName, @TempDir Path outputDirectory) throws URISyntaxException, IOException {
        // GIVEN meta-models, instance models, a valid query and an output path
        URI metaModelPath = getResource(Utils.MODELS);
        URI instanceModelPath = getResource(Utils.INSTANCES);
        URI query = getResource(Utils.QUERIES.resolve(queryName + ".nj"));

        Path output = outputDirectory.resolve(queryName + ".xmi");

        String metaModelPathArg = "--meta-model-path=" + metaModelPath;
        String instanceModelPathArg = "--instance-model-path=" + instanceModelPath;
        String transformArg = "--transform=" + output;
        String queryArg = Path.of(query).toString();

        // WHEN transforming the instance models
        int exitCode = new CommandLine(new Main()).execute(new String[] { metaModelPathArg, instanceModelPathArg, transformArg, queryArg });

        // THEN the correct view is generated (ignoring the order of view elements)
        assertEquals(0, exitCode);
        
        URI resultModel = getResource(Utils.MODELS.resolve(queryName + ".ecore"));
        URI result = getResource(Utils.RESULTS.resolve(queryName + ".xmi"));

        Stream<Diff> differences = compareInstanceFiles(Path.of(resultModel), Path.of(result), output)
            .getDifferences()
            .stream()
            .filter(diff -> diff.getKind() != DifferenceKind.MOVE);

        assertTrue(differences.findAny().isEmpty());
    }

}
