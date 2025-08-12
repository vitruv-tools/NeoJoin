package tools.vitruv.neojoin.cli.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tools.vitruv.neojoin.cli.integration.Utils.getResource;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.ValueSource;

import picocli.CommandLine;
import tools.vitruv.neojoin.cli.Main;

public class CheckTest {

    static List<String> validQueries = List.of("actor-rating", "books-on-tape", "customer-borrowings", "movies");

    /**
     * {@code java -jar cli.jar --meta-model-path=<meta-model-path> <query>}
     */
    @ParameterizedTest
    @FieldSource("validQueries")
    public void testCheckQuery(String queryName) {
        // GIVEN meta-models and a valid query on them
        URI metaModelPath = getResource(Utils.MODELS);
        URI query = getResource(Utils.QUERIES.resolve(queryName + ".nj"));

        String metaModelPathArg = "--meta-model-path=" + metaModelPath;
        String queryArg = Path.of(query).toString();

        // WHEN checking the query
        int exitCode = new CommandLine(new Main()).execute(new String[] { metaModelPathArg, queryArg });
        
        // THEN the query is accepted
        assertEquals(0, exitCode);
    }

    /**
     * {@code java -jar cli.jar --meta-model-path=<meta-model-path> <query>}
     */
    @ParameterizedTest
    @ValueSource(strings = { "invalid" })
    public void testCheckQueryFail(String queryName) {
        // GIVEN meta-models and an invalid query
        URI metaModelPath = getResource(Utils.MODELS);
        URI query = getResource(Utils.QUERIES.resolve(queryName + ".nj"));

        String metaModelPathArg = "--meta-model-path=" + metaModelPath;
        String queryArg = Path.of(query).toString();

        // WHEN checking the query
        int exitCode = new CommandLine(new Main()).execute(new String[] { metaModelPathArg, queryArg });
        
        // THEN the query is rejected
        assertEquals(1, exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = { "actor-rating" })
    public void testAbsoluteMetaModelPath(String queryName) {
        // GIVEN meta-models and a valid query on them specified using absolute paths instead of URIs
        URI metaModelPath = getResource(Utils.MODELS);
        URI query = getResource(Utils.QUERIES.resolve(queryName + ".nj"));

        String metaModelPathArg = "--meta-model-path=" + Path.of(metaModelPath);
        String queryArg = Path.of(query).toString();

        // WHEN checking the query
        int exitCode = new CommandLine(new Main()).execute(new String[] { metaModelPathArg, queryArg });
        
        // THEN the query is accepted
        assertEquals(0, exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = { "actor-rating" })
    public void testRelativePaths(String queryName) {
        // GIVEN meta-models and a valid query on them specified using relative paths instead of URIs
        URI metaModelPath = getResource(Utils.MODELS);
        URI query = getResource(Utils.QUERIES.resolve(queryName + ".nj"));

        Path relativeMetaModelPath = Path.of("").toAbsolutePath().relativize(Path.of(metaModelPath));
        Path relativeQuery = Path.of("").toAbsolutePath().relativize(Path.of(query));

        String metaModelPathArg = "--meta-model-path=" + relativeMetaModelPath;
        String queryArg = relativeQuery.toString();

        // WHEN checking the query
        int exitCode = new CommandLine(new Main()).execute(new String[] { metaModelPathArg, queryArg });
        
        // THEN the query is accepted
        assertEquals(0, exitCode);
    }
}
