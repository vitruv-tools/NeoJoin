package tools.vitruv.neojoin.frontend.cli.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.vitruv.neojoin.frontend.cli.integration.Utils.getResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;
import tools.vitruv.neojoin.frontend.cli.Main;

class TransformErrorTest {

    private String testName;

    @BeforeEach
    void rememberTestName(TestInfo testInfo) {
        testName = testInfo.getTestMethod().orElseThrow().getName();
    }

    @Test
    void failingExpressionIsReportedWithItsLocationInTheQuery(@TempDir Path directory) throws IOException {
        var query = directory.resolve("failing.nj");
        Files.writeString(query, """
            export failing to "http://example.org/failing"

            import "http://example.org/restaurant"

            from Restaurant r create {
                ratio := r.numEmployees / 0
            }
            """);

        var result = transform(query, getResource(Utils.INSTANCES), directory.resolve("out.xmi"));

        assertEquals(1, result.exitCode());
        assertTrue(
            result.err().contains("[ERROR] Failed to transform models: error during expression evaluation"),
            result.err()
        );
        assertTrue(result.err().contains("failing.nj:6:"), result.err());
    }

    @Test
    void errorInInstanceModelPathStopsTheTransformation(@TempDir Path directory) {
        var output = directory.resolve("out.xmi");

        var result = transform(testFolder().resolve("restaurants.nj"), testFolder().resolve("instances"), output);

        assertEquals(1, result.exitCode());
        assertTrue(
            result.err().contains("[ERROR] Found several instance models of the meta-model 'http://example.org/restaurant'"),
            result.err()
        );
        assertFalse(Files.exists(output));
    }

    @Test
    void warningInInstanceModelPathDoesNotStopTheTransformation(@TempDir Path directory) {
        var output = directory.resolve("out.xmi");

        var result = transform(testFolder().resolve("restaurants.nj"), testFolder().resolve("instances"), output);

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(result.err().contains("[WARNING] Ignoring the instance model"), result.err());
        assertTrue(Files.exists(output));
    }

    /**
     * Returns the resource folder of the running test, which contains its query and instance models.
     */
    private Path testFolder() {
        return getResource(Path.of("transform-errors", testName));
    }

    private static Result transform(Path query, Path instanceModelPath, Path output) {
        var originalErr = System.err;
        var err = new ByteArrayOutputStream();
        try (var errStream = new PrintStream(err, true)) {
            System.setErr(errStream);
            var exitCode = new CommandLine(new Main()).execute(
                "--meta-model-path=" + getResource(Utils.MODELS),
                "--instance-model-path=" + instanceModelPath,
                "--transform=" + output,
                query.toString()
            );
            return new Result(exitCode, err.toString());
        } finally {
            System.setErr(originalErr);
        }
    }

    private record Result(int exitCode, String err) {}

}
