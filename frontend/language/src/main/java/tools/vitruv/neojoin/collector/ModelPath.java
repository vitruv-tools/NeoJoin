package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A model path: a semicolon separated list of entries. Each entry is a model file, an {@link #isSupportedArchive
 * archive} or a directory, which is searched recursively for model files and archives.
 */
final class ModelPath {

    private final List<String> entries;

    ModelPath(String path) {
        this.entries = List.of(path.split(";"));
    }

    /**
     * Returns the files with the given extension, in the order of the path and, within a directory or archive, sorted
     * by path. Entries that are invalid or do not exist, directories that cannot be read and archives that cannot be
     * opened are reported as issues and skipped.
     *
     * @param extension file extension of the models
     * @param issues    list to add the issues to
     * @return the URIs of the model files
     */
    List<URI> findFiles(String extension, List<CollectorIssue> issues) {
        List<URI> files = new ArrayList<>();
        for (var entry : entries) {
            files.addAll(findFiles(entry, extension, issues));
        }
        return files;
    }

    /**
     * Returns the model files of a path entry (a model file, an archive or a directory).
     */
    private static List<URI> findFiles(String entry, String extension, List<CollectorIssue> issues) {
        Path fileOrDirectory;
        try {
            // without `.` and `..` segments, which EMF removes when it resolves a relative reference; the file would
            // be loaded a second time under the other URI
            fileOrDirectory = Path.of(entry).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            issues.add(new CollectorIssue.LoadFailure(entry, "the path is invalid (%s).".formatted(e.getReason())));
            return List.of();
        }

        if (!Files.exists(fileOrDirectory)) {
            issues.add(new CollectorIssue.LoadFailure(entry, "the file or directory does not exist."));
            return List.of();
        }

        try (var paths = Files.walk(fileOrDirectory)) {
            return paths
                .filter(Files::isRegularFile)
                .sorted()
                .flatMap(file -> isSupportedArchive(file)
                    ? findFilesInArchive(file, issues).stream()
                    : Stream.of(file))
                .map(ModelPath::toEmfUri)
                .filter(uri -> Objects.equals(uri.fileExtension(), extension))
                .toList();
        } catch (IOException | UncheckedIOException e) {
            // for example a directory without read permission
            issues.add(CollectorIssue.LoadFailure.of(entry, e));
            return List.of();
        }
    }

    private static boolean isSupportedArchive(Path file) {
        var name = file.getFileName().toString().toLowerCase();

        return name.endsWith(".jar");
    }

    private static List<Path> findFilesInArchive(Path archive, List<CollectorIssue> issues) {
        try (FileSystem archiveFs = FileSystems.newFileSystem(archive, Collections.emptyMap())) {
            List<Path> files = new ArrayList<>();
            for (var root : archiveFs.getRootDirectories()) {
                try (var entries = Files.walk(root)) {
                    // directory entries can look like models too, e.g.
                    // META-INF/maven/org.eclipse.emf/org.eclipse.emf.ecore/
                    entries.filter(Files::isRegularFile).forEach(files::add);
                }
            }

            files.sort(null);
            return files;
        } catch (IOException | ProviderNotFoundException e) {
            issues.add(CollectorIssue.LoadFailure.of(archive.toUri().toString(), e));
            return List.of();
        }
    }

    /**
     * Entries of an archive have {@code jar:} URIs. EMF loads those through {@code java.net.JarURLConnection}, which
     * caches the opened archive and keeps it locked (on Windows it cannot be deleted or replaced). EMF's own
     * {@code archive:} scheme closes the archive after reading the entry.
     */
    private static URI toEmfUri(Path path) {
        var uri = path.toUri().toString();
        return URI.createURI(uri.startsWith("jar:") ? "archive:" + uri.substring("jar:".length()) : uri);
    }

}
