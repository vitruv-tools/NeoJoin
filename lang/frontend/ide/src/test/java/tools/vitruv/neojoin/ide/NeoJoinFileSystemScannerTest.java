package tools.vitruv.neojoin.ide;

import com.google.inject.Guice;
import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NeoJoinFileSystemScannerTest {

    private static List<String> scan(Path workspace, Set<String> excludedDirectories) {
        var scanner = new NeoJoinFileSystemScanner(excludedDirectories);
        Guice.createInjector().injectMembers(scanner); // as Main's toInstance binding does
        List<String> scanned = new ArrayList<>();
        scanner.scan(URI.createFileURI(workspace.toString()), uri -> scanned.add(uri.toFileString()));
        return scanned;
    }

    private static Path createQueryAndCopy(Path workspace) throws IOException {
        var source = Files.createDirectories(workspace.resolve("src/main/resources/views")).resolve("query.nj");
        Files.writeString(source, "");
        var copy = Files.createDirectories(workspace.resolve("target/classes/views")).resolve("query.nj");
        Files.writeString(copy, "");
        return source;
    }

    /**
     * Maven copies queries to {@code target/classes}; indexing the copy too would define every query twice.
     */
    @Test
    void skipsExcludedDirectories(@TempDir Path workspace) throws IOException {
        var source = createQueryAndCopy(workspace);

        var scanned = scan(workspace, Set.of("target"));

        assertThat(scanned).anyMatch(path -> Path.of(path).equals(source));
        assertThat(scanned).noneMatch(path -> Path.of(path).startsWith(workspace.resolve("target")));
    }

    @Test
    void scansEverythingWithoutExcludedDirectories(@TempDir Path workspace) throws IOException {
        createQueryAndCopy(workspace);

        var scanned = scan(workspace, Set.of());

        assertThat(scanned).anyMatch(path -> Path.of(path).startsWith(workspace.resolve("target")));
    }

    @Test
    void excludedDirectoriesAreSeparatedBySemicolons() {
        var main = new Main();
        new CommandLine(main).parseArgs("--meta-model-path", ".", "--excluded-directories", "target;.git");

        assertThat(main.excludedDirectories).containsExactly("target", ".git");
    }

}
