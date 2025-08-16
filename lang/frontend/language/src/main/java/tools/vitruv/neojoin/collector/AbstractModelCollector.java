package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static tools.vitruv.neojoin.utils.Assertions.require;

/**
 * Collects models based on a search paths. Supports both {@link PackageModelCollector meta-models} and
 * {@link InstanceModelCollector instance-models}.
 * <p>
 * A search path is a semicolon separated list of paths. Each path can either point directly to a model file
 * or to a directory which is then searched recursively for models. All files with the specified
 * {@link #fileExtension() file extension} are considered models and will be
 * {@link ResourceSet#getResource(URI, boolean) loaded}.
 */
public abstract class AbstractModelCollector {

    private final List<Path> paths;

    public AbstractModelCollector(String searchPathString) {
        this.paths = parseSearchPathString(searchPathString);
    }

    private static List<Path> parseSearchPathString(String pathString) {
        return Arrays.stream(pathString.split(";"))
            .map(Path::of)
            .toList();
    }

    protected abstract String fileExtension();

    protected Stream<Resource> collectResourcesAsStream(ResourceSet resourceSet) {
        return paths.stream()
            .flatMap(path -> getContainedFiles(path).stream())
            .map(uri -> resourceSet.getResource(uri, true));
    }

	private List<URI> getContainedFiles(Path fileOrDirectory) {
		var extension = fileExtension();

		try (var paths = Files.walk(fileOrDirectory)) {
			return paths
				.filter(Files::isRegularFile)
				.map(path -> URI.createURI(path.toUri().toString()))
				.filter(uri -> Objects.equals(uri.fileExtension(), extension))
				.toList();
		} catch (NoSuchFileException e) {
			throw new IllegalArgumentException("File or directory does not exist: " + fileOrDirectory);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
