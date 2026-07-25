package tools.vitruv.neojoin.cli.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.vitruv.neojoin.cli.integration.Utils.compareInstanceFiles;
import static tools.vitruv.neojoin.cli.integration.Utils.getResource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;
import tools.vitruv.neojoin.cli.Main;

class ParameterTransformTest {

    @BeforeAll
    static void setupRegistry() {
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("xmi")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        }
    }

    @Test
    void testTransformWithParameters(@TempDir Path outputDirectory) throws URISyntaxException, IOException {
        var metaModelPath = getResource(Utils.MODELS);
        var instanceModelPath = getResource(Utils.INSTANCES);
        var query = getResource(Utils.QUERIES.resolve("pizza-param.nj"));
        var selectedFoods = getResource(Utils.PARAMS.resolve("selected-foods.xmi"));
        Path output = outputDirectory.resolve("pizza-param.xmi");

        int exitCode = new CommandLine(new Main()).execute(
            "--meta-model-path=" + metaModelPath,
            "--instance-model-path=" + instanceModelPath,
            "--transform=" + output,
            "--parameters=namePrefix=Pizzeria,minEmployees=2,selectedFoods=" + selectedFoods,
            query.toString()
        );

        assertEquals(0, exitCode);

        var resultModel = getResource(Utils.MODELS.resolve("pizza-param.ecore"));
        var expected = getResource(Utils.RESULTS.resolve("pizza-param.xmi"));

        Stream<Diff> differences = compareInstanceFiles(resultModel, expected, output)
            .getDifferences()
            .stream()
            .filter(diff -> diff.getKind() != DifferenceKind.MOVE);
        assertTrue(differences.findAny().isEmpty());
    }

    @Test
    void testTransformWrongParameterTypeFails(@TempDir Path outputDirectory) throws URISyntaxException {
        var metaModelPath = getResource(Utils.MODELS);
        var instanceModelPath = getResource(Utils.INSTANCES);
        var query = getResource(Utils.QUERIES.resolve("pizza-param.nj"));
        var selectedFoods = getResource(Utils.PARAMS.resolve("selected-foods.xmi"));
        Path output = outputDirectory.resolve("pizza-param-wrong-type.xmi");

        int exitCode = new CommandLine(new Main()).execute(
            "--meta-model-path=" + metaModelPath,
            "--instance-model-path=" + instanceModelPath,
            "--transform=" + output,
            "--parameters=namePrefix=Pizzeria,selectedFoods=" + selectedFoods + ",minEmployees=notAnInt",
            query.toString()
        );

        assertNotEquals(0, exitCode);
    }

    @Test
    void testTransformXmiNotFoundFails(@TempDir Path outputDirectory) throws URISyntaxException {
        var metaModelPath = getResource(Utils.MODELS);
        var instanceModelPath = getResource(Utils.INSTANCES);
        var query = getResource(Utils.QUERIES.resolve("pizza-param.nj"));
        Path output = outputDirectory.resolve("pizza-param-missing-xmi.xmi");

        int exitCode = new CommandLine(new Main()).execute(
            "--meta-model-path=" + metaModelPath,
            "--instance-model-path=" + instanceModelPath,
            "--transform=" + output,
            "--parameters=namePrefix=Pizzeria,selectedFoods=/nonexistent/path/foods.xmi",
            query.toString()
        );

        assertNotEquals(0, exitCode);
    }

}
