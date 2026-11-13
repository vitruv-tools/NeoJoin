package tools.vitruv.neojoin;

import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SourceLocationTest {

    /**
     * The space checks that the percent-encoding of the EMF URI is not kept in the path.
     */
    @Test
    void absoluteFileIsDisplayedAsItsFileUri(@TempDir Path directory) {
        var query = directory.resolve("my queries").resolve("query.nj");

        var display = SourceLocation.unknown(URI.createFileURI(query.toString())).display();

        assertThat(display).isEqualTo(query.toUri() + ":1:1");
    }

    @Test
    void relativeFileIsResolvedAgainstTheWorkingDirectory() {
        var display = SourceLocation.unknown(URI.createFileURI("query.nj")).display();

        assertThat(display).isEqualTo(Path.of("query.nj").toAbsolutePath().toUri() + ":1:1");
    }

}
