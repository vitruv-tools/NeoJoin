package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Collects models based on a search paths. Supports both {@link PackageModelCollector meta-models} and
 * {@link InstanceModelCollector instance-models}.
 * <p>
 * A search path is a semi-colon separted list of URIs (currently only file URIs). The URI can point directly to a
 * model file or to a path which is then searched recursively for models.
 */
public abstract class AbstractModelCollector {

	private final List<URI> paths;

	public AbstractModelCollector(String searchPathString) {
		this.paths = parseSearchPathString(searchPathString);
	}

	private static List<URI> parseSearchPathString(String pathString) {
		return Arrays.stream(pathString.split(";"))
			.map(URI::createURI)
			.toList();
	}

	protected abstract Predicate<URI> getFilter();

	protected Stream<Resource> collectResourcesAsStream(ResourceSet resourceSet) {
		return paths.stream()
			.flatMap(p -> URIAccessor.Registry.get(p).getContainedFiles(p, getFilter()).stream())
			.map(uri -> resourceSet.getResource(uri, true));
	}

}
