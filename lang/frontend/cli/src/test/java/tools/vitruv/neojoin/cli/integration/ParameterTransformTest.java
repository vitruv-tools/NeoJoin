package tools.vitruv.neojoin.cli.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.vitruv.neojoin.cli.integration.Utils.compareInstanceFiles;
import static tools.vitruv.neojoin.cli.integration.Utils.getResource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import picocli.CommandLine;
import tools.vitruv.neojoin.cli.Main;

class ParameterTransformTest {

    record TransformCase(
        String queryName,
        String paramKey,
        String paramValue, // literal for EDataType, xmi filename for EClass/EList
        boolean isFile // is it a file in the params directory?
    ) {}

    static List<TransformCase> happyPathCases = List.of(
        new TransformCase("pizza-param-employees", "minEmployees", "2", false), // EInt param
        new TransformCase("pizza-param-name", "namePrefix", "Pizzeria", false), // EString param
        new TransformCase("pizza-param-restaurant", "restaurant", "restaurant-single.xmi", true), // EClass param
        new TransformCase("pizza-param-foods", "selectedFoods", "selected-foods.xmi", true) // EList<> param
    );

    @BeforeAll
    static void setupRegistry() {
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("xmi")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        }
    }

    @ParameterizedTest
    @FieldSource("happyPathCases")
    void testTransformWithParameter(TransformCase c, @TempDir Path outputDirectory) throws URISyntaxException, IOException {
        var metaModelPath = getResource(Utils.MODELS);
        var instanceModelPath = getResource(Utils.INSTANCES);
        var query = getResource(Utils.QUERIES.resolve(c.queryName() + ".nj"));
        Path output = outputDirectory.resolve(c.queryName() + ".xmi");

        String resolvedParamValue = c.isFile()
            ? getResource(Utils.PARAMS.resolve(c.paramValue())).toString()
            : c.paramValue();

        int exitCode = new CommandLine(new Main()).execute(
            "--meta-model-path=" + metaModelPath,
            "--instance-model-path=" + instanceModelPath,
            "--transform=" + output,
            "--parameters=" + c.paramKey() + "=" + resolvedParamValue,
            query.toString()
        );

        assertEquals(0, exitCode);

        var resultModel = getResource(Utils.MODELS.resolve("pizza.ecore"));
        var expected = getResource(Utils.RESULTS.resolve(c.queryName() + ".xmi"));

        Stream<Diff> differences = compareInstanceFiles(resultModel, expected, output)
            .getDifferences()
            .stream()
            .filter(diff -> diff.getKind() != DifferenceKind.MOVE);
        assertTrue(differences.findAny().isEmpty());
    }

    // When -p is omitted for a declared parameter the CLI sets it to null.
    // Filtering is bypassed, so all source instances appear in the output.
    @Test
    void testTransformMissingParameterBypassesFilter(@TempDir Path outputDirectory) throws URISyntaxException, IOException {
        var metaModelPath = getResource(Utils.MODELS);
        var instanceModelPath = getResource(Utils.INSTANCES);
        var query = getResource(Utils.QUERIES.resolve("pizza-param-employees.nj"));
        Path output = outputDirectory.resolve("pizza-param-employees-no-param.xmi");

        int exitCode = new CommandLine(new Main()).execute(
            "--meta-model-path=" + metaModelPath,
            "--instance-model-path=" + instanceModelPath,
            "--transform=" + output,
            query.toString()
        );

        assertEquals(0, exitCode);

        var resultModel = getResource(Utils.MODELS.resolve("pizza.ecore"));
        var expected = getResource(Utils.RESULTS.resolve("pizza-param-employees-no-param.xmi"));

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
        var query = getResource(Utils.QUERIES.resolve("pizza-param-employees.nj"));
        Path output = outputDirectory.resolve("pizza-param-employees-wrong-type.xmi");

        int exitCode = new CommandLine(new Main()).execute(
            "--meta-model-path=" + metaModelPath,
            "--instance-model-path=" + instanceModelPath,
            "--transform=" + output,
            "--parameters=minEmployees=notAnInt",
            query.toString()
        );

        assertNotEquals(0, exitCode);
    }

    @Test
    void testTransformXmiNotFoundFails(@TempDir Path outputDirectory) throws URISyntaxException {
        var metaModelPath = getResource(Utils.MODELS);
        var instanceModelPath = getResource(Utils.INSTANCES);
        var query = getResource(Utils.QUERIES.resolve("pizza-param-restaurant.nj"));
        Path output = outputDirectory.resolve("pizza-param-missing-xmi.xmi");

        int exitCode = new CommandLine(new Main()).execute(
            "--meta-model-path=" + metaModelPath,
            "--instance-model-path=" + instanceModelPath,
            "--transform=" + output,
            "--parameters=restaurant=/nonexistent/path/restaurant.xmi",
            query.toString()
        );

        assertNotEquals(0, exitCode);
    }

}
