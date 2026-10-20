package tools.vitruv.neojoin.cli.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.vitruv.neojoin.cli.integration.Utils.getResource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import picocli.CommandLine;
import tools.vitruv.neojoin.cli.Main;

/**
 * End-to-end test of the conformance check: the view type generated from a query is validated against the reference
 * meta-model given in the query's {@code conforms to} clause.
 */
public class ConformanceTest {

    private final PrintStream originalErr = System.err;
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @BeforeEach
    void captureErr() {
        System.setErr(new PrintStream(err));
    }

    @AfterEach
    void restoreErr() {
        System.setErr(originalErr);
    }

    /**
     * {@code java -jar cli.jar --meta-model-path=<meta-model-path> <query>}
     */
    @Test
    public void testConformingViewType() {
        // GIVEN a query whose view type conforms to its reference meta-model
        // WHEN checking the query
        int exitCode = check("movies-conforming");

        // THEN the query is accepted
        assertEquals(0, exitCode, err.toString());
    }

    /**
     * {@code java -jar cli.jar --meta-model-path=<meta-model-path> <query>}
     */
    @Test
    public void testNonConformingViewType() {
        // GIVEN a query whose view type does not conform to its reference meta-model
        // WHEN checking the query
        int exitCode = check("actor-rating-nonconforming");

        // THEN the query is rejected, naming the feature required by the reference
        assertEquals(1, exitCode);
        assertTrue(err.toString().contains("nationality"), err.toString());
    }

    private int check(String queryName) {
        var metaModelPath = getResource(Utils.MODELS);
        var query = getResource(Utils.QUERIES.resolve(queryName + ".nj"));

        return new CommandLine(new Main()).execute("--meta-model-path=" + metaModelPath, query.toString());
    }

}
