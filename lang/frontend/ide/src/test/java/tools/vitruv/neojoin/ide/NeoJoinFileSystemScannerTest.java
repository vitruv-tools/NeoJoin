package tools.vitruv.neojoin.ide;

import com.google.inject.Guice;
import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NeoJoinFileSystemScannerTest {

    /**
     * Maven copies queries to {@code target/classes}; indexing the copy too would define every query twice.
     */
    @Test
    void skipsBuildOutputDirectories(@TempDir Path workspace) throws IOException {
        var source = Files.createDirectories(workspace.resolve("src/main/resources/views")).resolve("query.nj");
        Files.writeString(source, "");
        var copy = Files.createDirectories(workspace.resolve("target/classes/views")).resolve("query.nj");
        Files.writeString(copy, "");

        var scanner = Guice.createInjector().getInstance(NeoJoinFileSystemScanner.class);
        List<String> scanned = new ArrayList<>();
        scanner.scan(URI.createFileURI(workspace.toString()), uri -> scanned.add(uri.toFileString()));

        assertThat(scanned).anyMatch(path -> Path.of(path).equals(source));
        assertThat(scanned).noneMatch(path -> Path.of(path).startsWith(workspace.resolve("target")));
    }

}
