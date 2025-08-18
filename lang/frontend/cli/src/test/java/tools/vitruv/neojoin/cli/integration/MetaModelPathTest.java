package tools.vitruv.neojoin.cli.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tools.vitruv.neojoin.cli.integration.Utils.getResource;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import picocli.CommandLine;
import tools.vitruv.neojoin.cli.Main;

public class MetaModelPathTest {

    // actor-rating: meta-model in top-level directory
    // devices: meta-model in sub directory
    // services: meta-model in JAR file
    static List<String> validQueries = List.of("actor-rating", "devices", "services");

    @ParameterizedTest
    @FieldSource("validQueries")
    public void testCheckQuery(String queryName) {
        // GIVEN meta-models and a valid query on them
        var metaModelPath = getResource(Utils.MODELS);
        var query = getResource(Utils.QUERIES.resolve(queryName + ".nj"));

        String metaModelPathArg = "--meta-model-path=" + metaModelPath;
        String queryArg = query.toString();

        // WHEN checking the query
        int exitCode = new CommandLine(new Main()).execute(new String[] { metaModelPathArg, queryArg });
        
        // THEN the query is accepted
        assertEquals(0, exitCode);
    }

}
