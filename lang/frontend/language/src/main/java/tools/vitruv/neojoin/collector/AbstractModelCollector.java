package tools.vitruv.neojoin.collector;

import tools.vitruv.neojoin.utils.Utils;
import static tools.vitruv.neojoin.utils.Utils.executeCatchingIOException;
import static tools.vitruv.neojoin.utils.Utils.collectFailingFast;
import tools.vitruv.neojoin.utils.Result;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

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

    protected Stream<Result<Resource, IOException>> collectResourcesAsStream(ResourceSet resourceSet) {
        return paths.stream()
            .<Result<URI, IOException>>flatMap(path ->
                    executeCatchingIOException(() -> getContainedFiles(path).stream())
                        .fold(it -> it.map(Result::of), e -> Stream.of(Result.fail(e)))
                    )
            .map(result -> result.map(uri -> resourceSet.getResource(uri, true)));
    }

	private List<URI> getContainedFiles(Path fileOrDirectory) throws IOException {
		var extension = fileExtension();

		try (var paths = Files.walk(fileOrDirectory)) {
			return paths
				.filter(Files::isRegularFile)
                .<Result<Path, IOException>>flatMap(file ->
                    executeCatchingIOException(() ->
                            isSupportedArchive(file) ? getContainedFilesInArchive(file).stream() : Stream.of(file))
                        .fold(it -> it.map(Result::of), e -> Stream.of(Result.fail(e)))
                )
                .collect(
                    collectFailingFast(
                        Collectors.mapping(
                            path -> URI.createURI(path.toUri().toString()),
                            Collectors.filtering(uri -> Objects.equals(uri.fileExtension(), extension), Collectors.toList())
                        )
                    )
                )
                .valueUnsafe();
		} catch (NoSuchFileException e) {
			throw new NoSuchFileException(fileOrDirectory.toString(), null, e.getMessage());
		}
	}

    public static boolean isSupportedArchive(Path file) {
        var name = file.getFileName().toString().toLowerCase();

        return name.endsWith(".jar");
    }

    private static List<Path> getContainedFilesInArchive(Path archive) throws IOException {
        try (FileSystem archiveFs = FileSystems.newFileSystem(archive, Collections.emptyMap())) {
            return Utils.streamOf(archiveFs.getRootDirectories().iterator())
                .<Result<Path, IOException>>flatMap(root ->
                    executeCatchingIOException(() -> Files.walk(root))
                        .fold(it -> it.map(Result::of), e -> Stream.of(Result.fail(e))))
                .collect(collectFailingFast(Collectors.toList()))
                .valueUnsafe();
        }
    }

}
