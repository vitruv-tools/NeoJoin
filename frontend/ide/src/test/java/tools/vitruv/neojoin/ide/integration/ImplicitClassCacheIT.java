package tools.vitruv.neojoin.ide.integration;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The implicit classes of a query are cached per resource. Only a language server edits and validates a document in
 * the same resource, so only here a stale cache shows. Runs against the shaded jar like {@link NeoJoinIdeServerIT}.
 */
class ImplicitClassCacheIT {

    private static final String HEADER = """
        export package to "http://example.com"

        import "http://example.org/restaurant"

        """;

    private static final String QUERY_WITH_IMPLICIT_CLASS = HEADER + """
        from Restaurant r create {
            r.sells
            first: Food := r.sells.head
        }
        """;

    private static final String QUERY_WITHOUT_IMPLICIT_CLASS = HEADER + """
        from Restaurant r create {
            first: Food := r.sells.head
        }
        """;

    @TempDir
    Path directory;

    @Test
    void implicitClassTypeFollowsEditsOfTheDocument()
        throws IOException, ExecutionException, InterruptedException, TimeoutException {
        try (InputStream model = getClass().getResourceAsStream("/models/restaurant.ecore")) {
            Files.copy(Objects.requireNonNull(model), directory.resolve("restaurant.ecore"));
        }
        var server = InProcessLanguageServer.start(directory);
        var uri = server.open(directory.resolve("query.nj"), QUERY_WITH_IMPLICIT_CLASS);
        assertThat(errorsOf(server.diagnostics(uri))).isEmpty();

        server.changeDocument(uri, QUERY_WITHOUT_IMPLICIT_CLASS);
        assertThat(errorsOf(server.diagnostics(uri))).containsExactly("Food cannot be resolved.");

        server.changeDocument(uri, QUERY_WITH_IMPLICIT_CLASS);
        assertThat(errorsOf(server.diagnostics(uri))).isEmpty();
    }

    private static List<String> errorsOf(List<Diagnostic> diagnostics) {
        return diagnostics.stream()
            .filter(diagnostic -> diagnostic.getSeverity() == DiagnosticSeverity.Error)
            .map(diagnostic -> diagnostic.getMessage().getLeft())
            .toList();
    }

}
