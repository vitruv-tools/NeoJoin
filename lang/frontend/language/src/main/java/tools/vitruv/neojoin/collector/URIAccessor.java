package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;

import java.util.List;
import java.util.function.Predicate;

/**
 * Handler to retrieve contained files for a given URI.
 */
public interface URIAccessor {

	/**
	 * @return {@code true} if this accessor can handle the given URI, {@code false} otherwise.
	 */
	boolean canHandle(URI uri);

	/**
	 * Returns a list of URIs of files that are contained in the given path and match the given filter. If the given
	 * path is a file, the file itself will be returned if it matches the filter.
	 *
	 * @param path   path to a file or directory
	 * @param filter filter to apply to the contained files, e.g. to filter by file extension
	 * @return list of URIs of contained files that match the filter
	 */
	List<URI> getContainedFiles(URI path, Predicate<URI> filter);

	/**
	 * Registry for {@link URIAccessor} implementations.
	 */
	Registry Registry = new Registry();

	class Registry {

		private Registry() {}

		private final List<URIAccessor> accessors = List.of(new FileURIAccessor());

		/**
		 * Returns the first {@link URIAccessor} that can handle the given URI.
		 *
		 * @throws IllegalArgumentException if no accessor can handle the URI
		 */
		public URIAccessor get(URI uri) {
			return accessors.stream()
				.filter(a -> a.canHandle(uri))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No handler for URI: " + uri));
		}

	}

}
