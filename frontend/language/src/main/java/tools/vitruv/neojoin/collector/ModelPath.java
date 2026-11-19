package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A model path: a semicolon separated list of entries. Each entry is a model file, an {@link #isSupportedArchive
 * archive} or a directory, which is searched recursively for model files and archives.
 */
final class ModelPath {

    private final List<String> entries;

    private final String name;

    /**
     * @param path the entries separated by semicolons; empty entries (a leading or doubled separator, an empty path)
     *             are ignored, because they would be the working directory
     * @param name name of the path in messages, for example "meta-model path"
     */
    ModelPath(String path, String name) {
        this.entries = Stream.of(path.split(";"))
            .filter(entry -> !entry.isEmpty())
            .toList();
        this.name = name;
    }

    /**
     * Returns the files with the given extension, in the order of the path and, within a directory or archive, sorted
     * by path. Entries that are invalid, do not exist or are files with another extension, directories that cannot be
     * read and archives that cannot be opened are reported as issues and skipped. A file that several entries contain
     * (a directory and a file in it) is returned once, at its first position.
     *
     * @param extension file extension of the models
     * @param issues    list to add the issues to
     * @return the URIs of the model files
     */
    List<URI> findFiles(String extension, List<CollectorIssue> issues) {
        Set<URI> files = new LinkedHashSet<>();
        for (var entry : entries) {
            files.addAll(findFiles(entry, extension, issues));
        }
        return List.copyOf(files);
    }

    /**
     * Returns the model files of a path entry (a model file, an archive or a directory).
     */
    private List<URI> findFiles(String entry, String extension, List<CollectorIssue> issues) {
        Path fileOrDirectory;
        try {
            // without `.` and `..` segments, which EMF removes when it resolves a relative reference; the file would
            // be loaded a second time under the other URI
            fileOrDirectory = Path.of(entry).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            issues.add(new CollectorIssue.InvalidPathEntry(
                name,
                entry,
                "the path is invalid (%s).".formatted(e.getReason())
            ));
            return List.of();
        }

        if (!Files.exists(fileOrDirectory)) {
            issues.add(new CollectorIssue.InvalidPathEntry(name, entry, "the file or directory does not exist."));
            return List.of();
        }

        // within a directory, files with other extensions are skipped silently
        if (Files.isRegularFile(fileOrDirectory)
            && !isSupportedArchive(fileOrDirectory)
            && !Objects.equals(toEmfUri(fileOrDirectory).fileExtension(), extension)) {
            issues.add(new CollectorIssue.InvalidPathEntry(
                name,
                entry,
                "the file does not have the extension '.%s'.".formatted(extension)
            ));
            return List.of();
        }

        List<Path> files;
        try {
            files = findRegularFiles(fileOrDirectory, issues);
        } catch (IOException e) {
            issues.add(CollectorIssue.LoadFailure.of(entry, e));
            return List.of();
        }

        return files.stream()
            .sorted()
            .flatMap(file -> isSupportedArchive(file)
                ? findFilesInArchive(file, issues).stream()
                : Stream.of(file))
            .map(ModelPath::toEmfUri)
            .filter(uri -> Objects.equals(uri.fileExtension(), extension))
            .toList();
    }

    /**
     * Returns the regular files in a directory and its subdirectories (or the file itself). A directory that cannot be
     * read is reported and skipped, the walk continues with the other directories.
     */
    private static List<Path> findRegularFiles(Path fileOrDirectory, List<CollectorIssue> issues) throws IOException {
        List<Path> files = new ArrayList<>();

        Files.walkFileTree(fileOrDirectory, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                // follows a symbolic link to a file, like a file in the directory
                if (Files.isRegularFile(file)) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exception) {
                var location = file.toUri().toString();
                issues.add(exception instanceof AccessDeniedException
                    ? new CollectorIssue.LoadFailure(location, "access denied.")
                    : CollectorIssue.LoadFailure.of(location, exception));
                return FileVisitResult.CONTINUE;
            }
        });

        return files;
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
