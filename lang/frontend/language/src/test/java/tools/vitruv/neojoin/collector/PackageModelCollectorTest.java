package tools.vitruv.neojoin.collector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PackageModelCollectorTest {

    /**
     * A directory entry whose name ends with the model extension (EMF's own jar has
     * {@code META-INF/maven/org.eclipse.emf/org.eclipse.emf.ecore/}) is not a model.
     */
    @Test
    void ignoresDirectoryEntriesOfArchives(@TempDir Path dir) throws IOException {
        var jar = dir.resolve("models.jar");
        try (var out = new ZipOutputStream(Files.newOutputStream(jar));
             InputStream model = Objects.requireNonNull(getClass().getResourceAsStream("/models/restaurant.ecore"))) {
            out.putNextEntry(new ZipEntry("META-INF/maven/org.example/org.example.ecore/"));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("model/restaurant.ecore"));
            model.transferTo(out);
            out.closeEntry();
        }

        var result = new PackageModelCollector(jar.toString()).collect();

        assertThat(result.left()).isEmpty();
        assertThat(result.right().keySet()).containsExactly("http://example.org/restaurant");
    }

}
