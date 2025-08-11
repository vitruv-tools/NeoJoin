package tools.vitruv.neojoin.cli.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tools.vitruv.neojoin.cli.integration.Utils.getResource;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.ValueSource;

import picocli.CommandLine;
import tools.vitruv.neojoin.cli.Main;

public class CheckTest {

    static List<String> validQueries = List.of("actor-rating", "books-on-tape", "customer-borrowings", "movies");

    @ParameterizedTest
    @FieldSource("validQueries")
    public void testCheckQuery(String queryName) throws URISyntaxException {
        // GIVEN meta-models and a valid query on them
        URL metaModelPath = getResource(Utils.MODELS);
        URL query = getResource(Utils.QUERIES.resolve(queryName + ".nj"));

        String metaModelPathArg = "--meta-model-path=" + metaModelPath.toURI();
        String queryArg = query.getPath();

        // WHEN checking the query
        int exitCode = new CommandLine(new Main()).execute(new String[] { metaModelPathArg, queryArg });
        
        // THEN the query is accepted
        assertEquals(0, exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = { "invalid" })
    public void testCheckQueryFail(String queryName) throws URISyntaxException {
        // GIVEN meta-models and an invalid query
        URL metaModelPath = getResource(Utils.MODELS);
        URL query = getResource(Utils.QUERIES.resolve(queryName + ".nj"));

        String metaModelPathArg = "--meta-model-path=" + metaModelPath.toURI();
        String queryArg = query.getPath();

        // WHEN checking the query
        int exitCode = new CommandLine(new Main()).execute(new String[] { metaModelPathArg, queryArg });
        
        // THEN the query is rejected
        assertEquals(1, exitCode);
    }

}
