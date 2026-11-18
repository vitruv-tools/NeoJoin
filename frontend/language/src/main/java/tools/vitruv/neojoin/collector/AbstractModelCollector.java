package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.xml.sax.SAXParseException;

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
 * Collects models based on a search paths. Supports both {@link PackageModelCollector meta-models} and
 * {@link InstanceModelCollector instance-models}.
 * <p>
 * A search path is a semicolon separated list of paths. Each path can either point directly to a model file
 * or to a directory which is then searched recursively for models. Models can also be located in
 * {@link #isSupportedArchive(Path) supported archives} on the search path. All files with the specified
 * {@link #fileExtension() file extension} are considered models and will be
 * {@link ResourceSet#getResource(URI, boolean) loaded}.
 */
public abstract class AbstractModelCollector {

    private final List<String> pathEntries;

    protected AbstractModelCollector(String searchPathString) {
        this.pathEntries = List.of(searchPathString.split(";"));
    }

    protected abstract String fileExtension();

    /**
     * The result of loading one file of the search path.
     */
    protected sealed interface LoadResult {

        URI uri();

        record Loaded(URI uri, Resource resource) implements LoadResult {}

        record Failed(URI uri, RuntimeException exception) implements LoadResult {}
    }

    /**
     * Loads the models on the search path into the given resource set, in the order of the search path and, within a
     * directory or archive, sorted by path. Path entries that are invalid or do not exist, directories that cannot be
     * read and archives that cannot be opened are reported as issues and skipped.
     *
     * @param resourceSet resource set to load the models into
     * @param issues      list to add the issues to
     * @return the result of each file, in the order of loading
     */
    protected List<LoadResult> loadResources(ResourceSet resourceSet, List<CollectorIssue> issues) {
        List<LoadResult> results = new ArrayList<>();
        for (var entry : pathEntries) {
            for (var uri : getContainedFiles(entry, issues)) {
                try {
                    results.add(new LoadResult.Loaded(uri, resourceSet.getResource(uri, true)));
                } catch (RuntimeException e) {
                    results.add(new LoadResult.Failed(uri, e));
                }
            }
        }
        return results;
    }

    /**
     * Returns the issue for a file that cannot be loaded.
     */
    protected static CollectorIssue loadFailure(LoadResult.Failed failed) {
        return new CollectorIssue.LoadFailure(failed.uri().toString(), describe(failed.exception()));
    }

    /**
     * EMF and the file system wrap the problem in several exceptions; the innermost one describes it.
     */
    private static String describe(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof SAXParseException parseException) {
            return "%s (line %d, column %d)".formatted(
                parseException.getMessage(),
                parseException.getLineNumber(),
                parseException.getColumnNumber()
            );
        }
        return String.valueOf(cause.getMessage());
    }

    /**
     * Returns the model files of a path entry (a model file, an archive or a directory).
     */
    private List<URI> getContainedFiles(String entry, List<CollectorIssue> issues) {
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

        var extension = fileExtension();
        try (var paths = Files.walk(fileOrDirectory)) {
            return paths
                .filter(Files::isRegularFile)
                .sorted()
                .flatMap(file -> isSupportedArchive(file)
                    ? getContainedFilesInArchive(file, issues).stream()
                    : Stream.of(file))
                .map(AbstractModelCollector::toEmfUri)
                .filter(uri -> Objects.equals(uri.fileExtension(), extension))
                .toList();
        } catch (IOException | UncheckedIOException e) {
            // for example a directory without read permission
            issues.add(new CollectorIssue.LoadFailure(entry, describe(e)));
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

    public static boolean isSupportedArchive(Path file) {
        var name = file.getFileName().toString().toLowerCase();

        return name.endsWith(".jar");
    }

    private static List<Path> getContainedFilesInArchive(Path archive, List<CollectorIssue> issues) {
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
            issues.add(new CollectorIssue.LoadFailure(archive.toUri().toString(), describe(e)));
            return List.of();
        }
    }

}
