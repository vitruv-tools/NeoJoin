package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import static tools.vitruv.neojoin.utils.Assertions.require;

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
				.map(FileURIAccessor::createURI)
				.filter(filter)
				.toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static URI createURI(Path path) {
		return URI.createFileURI(path.toAbsolutePath().normalize().toString());
	}

}
