package tools.vitruv.neojoin.cli.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.vitruv.neojoin.cli.integration.Utils.compareEcoreFiles;
import static tools.vitruv.neojoin.cli.integration.Utils.getResource;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import picocli.CommandLine;
import tools.vitruv.neojoin.cli.Main;

public class GenerateTest {

    static List<String> validQueries = List.of("actor-rating", "books-on-tape", "customer-borrowings", "movies");

    /**
     * {@code java -jar cli.jar --meta-model-path=<meta-model-path> --generate=<output> <query>}
     */
    @ParameterizedTest
    @FieldSource("validQueries")
    public void testGenerateMetaModel(String queryName, @TempDir Path outputDirectory) throws IOException {
        // GIVEN meta-models, a valid query and an output path
        URI metaModelPath = getResource(Utils.MODELS);
        URI query = getResource(Utils.QUERIES.resolve(queryName + ".nj"));

        Path output = outputDirectory.resolve(queryName + ".ecore");

        String metaModelPathArg = "--meta-model-path=" + metaModelPath;
        String generateArg = "--generate=" + output;
        String queryArg = Path.of(query).toString();

        // WHEN generating the view type
        int exitCode = new CommandLine(new Main()).execute(new String[] { metaModelPathArg, generateArg, queryArg });
        
        // THEN the correct view type is generated (ignoring the order of view type elements)
        assertEquals(0, exitCode);

        URI result = getResource(Utils.RESULTS.resolve(queryName + ".ecore"));

        Stream<Diff> differences = compareEcoreFiles(Path.of(result), output)
            .getDifferences()
            .stream()
            .filter(diff -> diff.getKind() != DifferenceKind.MOVE);

        assertTrue(differences.findAny().isEmpty());
    }

}
