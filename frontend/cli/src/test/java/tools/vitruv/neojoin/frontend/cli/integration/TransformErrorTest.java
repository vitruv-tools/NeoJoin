package tools.vitruv.neojoin.frontend.cli.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.vitruv.neojoin.frontend.cli.integration.Utils.getResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;
import tools.vitruv.neojoin.frontend.cli.Main;

class TransformErrorTest {

    @Test
    void failingExpressionIsReportedWithItsLocationInTheQuery(@TempDir Path directory)
        throws IOException, URISyntaxException {
        var query = directory.resolve("failing.nj");
        Files.writeString(query, """
            export failing to "http://example.org/failing"

            import "http://example.org/restaurant"

            from Restaurant r create {
                ratio := r.numEmployees / 0
            }
            """);

        var originalErr = System.err;
        var err = new ByteArrayOutputStream();
        int exitCode;
        try {
            System.setErr(new PrintStream(err, true));
            exitCode = new CommandLine(new Main()).execute(
                "--meta-model-path=" + getResource(Utils.MODELS),
                "--instance-model-path=" + getResource(Utils.INSTANCES),
                "--transform=" + directory.resolve("out.xmi"),
                query.toString()
            );
        } finally {
            System.setErr(originalErr);
        }

        assertEquals(1, exitCode);
        var output = err.toString();
        assertTrue(
            output.contains("[ERROR] Failed to transform models: error during expression evaluation"),
            output
        );
        assertTrue(output.contains("failing.nj:6:"), output);
    }

}
