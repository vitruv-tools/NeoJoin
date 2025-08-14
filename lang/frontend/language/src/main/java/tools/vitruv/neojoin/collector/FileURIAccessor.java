package tools.vitruv.neojoin.collector;

import static tools.vitruv.neojoin.utils.Assertions.require;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;

import tools.vitruv.neojoin.utils.Utils;

/**
 * Accessor for file URIs.
 */
public class FileURIAccessor implements URIAccessor {

    @Override
    public boolean canHandle(URI uri) {
        return uri.isFile();
    }

    @Override
    public List<URI> getContainedFiles(URI fileOrDirectory, Predicate<URI> filter) {
        require(canHandle(fileOrDirectory), () -> "Cannot handle URI: " + fileOrDirectory);

        var path = Path.of(java.net.URI.create(fileOrDirectory.toString()));
        require(Files.exists(path), () -> "Does not exist: " + fileOrDirectory);

        if (Files.isRegularFile(path)) {
            var uri = createURI(path);
            if (filter.test(uri)) {
                return List.of(uri);
            } else {
                return List.of();
            }
        }

        require(Files.isDirectory(path), () -> "Not a file or directory: " + fileOrDirectory);

        try (var paths = Files.walk(path)) {
            return paths
                .filter(Files::isRegularFile)
                .flatMap(file -> file.getFileName().toString().toLowerCase().endsWith(".jar") ? getContainedFilesInArchive(file).stream() : Stream.of(file))
                .map(FileURIAccessor::createURI)
                .filter(filter)
                .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Path> getContainedFilesInArchive(Path archive) {
        try (FileSystem archiveFs = FileSystems.newFileSystem(archive, Collections.emptyMap())) {
            return Utils.streamOf(archiveFs.getRootDirectories().iterator())
                .flatMap(root -> {
                    try {
                        return Files.walk(root);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
        } catch (IOException | ProviderNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static URI createURI(Path path) {
        return URI.createURI(path.toUri().toString());
    }

}
