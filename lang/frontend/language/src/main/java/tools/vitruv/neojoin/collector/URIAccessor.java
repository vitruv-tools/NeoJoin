package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;

import java.util.List;
import java.util.function.Predicate;

public interface URIAccessor {

	boolean canHandle(URI uri);

	List<URI> getContainedFiles(URI path, Predicate<URI> filter);

	Registry Registry = new Registry();

	class Registry {

		private Registry() {}

		private final List<URIAccessor> accessors = List.of(new FileURIAccessor());

		public URIAccessor get(URI uri) {
			return accessors.stream()
				.filter(a -> a.canHandle(uri))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No handler for URI: " + uri));
		}

	}

}
