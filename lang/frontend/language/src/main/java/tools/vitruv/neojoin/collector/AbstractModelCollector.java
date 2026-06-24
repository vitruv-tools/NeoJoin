package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import tools.vitruv.neojoin.utils.Result;
import tools.vitruv.neojoin.utils.Utils;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.Arrays;
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

    private final List<Path> paths;

    protected AbstractModelCollector(String searchPathString) {
        this.paths = parseSearchPathString(searchPathString);
    }

    private static List<Path> parseSearchPathString(String pathString) {
        return Arrays.stream(pathString.split(";"))
            .map(Path::of)
            .toList();
    }

    protected abstract String fileExtension();

    protected Stream<Result<Resource, Exception>> collectResourcesAsStream(ResourceSet resourceSet) {
        return paths.stream()
            .<Result<URI, Exception>> flatMap(path -> {
                var files = getContainedFiles(path);
                if (files instanceof Result.Success<List<URI>, Exception> success) {
                    return success.value().stream().map(Result::of);
                } else if (files instanceof Result.Failure<List<URI>, Exception> failure) {
                    return Stream.of(failure.cast());
                } else {
                    throw new RuntimeException(); // this should never happen
                }
            })
            .map(uriResult -> uriResult.bind(uri -> Result.ofCaught(() -> resourceSet.getResource(uri, true))));
    }

	private Result<List<URI>, Exception> getContainedFiles(Path fileOrDirectory) {
		var extension = fileExtension();

		try (var paths = Files.walk(fileOrDirectory)) {
			return Result.of(paths
				.filter(Files::isRegularFile)
                .flatMap(file -> isSupportedArchive(file) ? getContainedFilesInArchive(file).stream() : Stream.of(file))
				.map(path -> URI.createURI(path.toUri().toString()))
				.filter(uri -> Objects.equals(uri.fileExtension(), extension))
				.toList());
		} catch (NoSuchFileException e) {
			return Result.fail(new IllegalArgumentException("File or directory does not exist: " + fileOrDirectory));
		} catch (IOException e) {
			return Result.fail(e);
		}
	}

    public static boolean isSupportedArchive(Path file) {
        var name = file.getFileName().toString().toLowerCase();

        return name.endsWith(".jar");
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

}
