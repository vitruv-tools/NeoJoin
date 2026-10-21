package tools.vitruv.neojoin.cli.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.vitruv.neojoin.cli.integration.Utils.getResource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import picocli.CommandLine;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.cli.Main;

/**
 * End-to-end test of the conformance check: the view type generated from a query is validated against the reference
 * meta-model given in the query's {@code conforms to} clause.
 */
public class ConformanceTest {

    private final PrintStream originalErr = System.err;
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @BeforeEach
    void resetGlobalStateAndCaptureErr() {
        System.setErr(new PrintStream(err));
        // Xtext registers the validator in the global EValidator registry and keeps the first one per JVM, which
        // would carry the strictness of whichever test ran first. Dropping it lets each run register its own.
        EValidator.Registry.INSTANCE.remove(AstPackage.eINSTANCE);
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

    /**
     * {@code java -jar cli.jar --meta-model-path=<meta-model-path> --strictness=EXACT_MATCH <query>}
     */
    @Test
    public void testExactMatchReportsAdditionalStructure() {
        // GIVEN a query whose view type declares more than its reference meta-model
        // WHEN checking the query with the strictness EXACT_MATCH
        int exitCode = check("movies-conforming", "--strictness=EXACT_MATCH");

        // THEN the query is rejected because of the additional feature
        assertEquals(1, exitCode, err.toString());
        assertTrue(err.toString().contains("Rule 2"), err.toString());
    }

    private int check(String queryName, String... options) {
        var metaModelPath = getResource(Utils.MODELS);
        var query = getResource(Utils.QUERIES.resolve(queryName + ".nj"));

        var args = new ArrayList<String>();
        args.add("--meta-model-path=" + metaModelPath);
        args.addAll(List.of(options));
        args.add(query.toString());
        return new CommandLine(new Main()).execute(args.toArray(String[]::new));
    }

}
