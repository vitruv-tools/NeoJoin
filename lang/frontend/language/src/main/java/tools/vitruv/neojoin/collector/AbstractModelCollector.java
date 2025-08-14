package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Collects models based on a search paths. Supports both {@link PackageModelCollector meta-models} and
 * {@link InstanceModelCollector instance-models}.
 * <p>
 * A search path is a semi-colon separated list of URIs (currently only file URIs) or paths. The URI can
 * point directly to a model file or to a path which is then searched recursively for models.
 */
public abstract class AbstractModelCollector {

    private final List<URI> paths;

    public AbstractModelCollector(String searchPathString) {
        this.paths = parseSearchPathString(searchPathString);
    }

    private static List<URI> parseSearchPathString(String pathString) {
        return Arrays.stream(pathString.split(";"))
            .map(AbstractModelCollector::createURI)
            .toList();
    }

    private static URI createURI(String string) {
        try {
            URI uri = URI.createURI(string);
            if (uri.scheme() == null || URIAccessor.Registry.get(uri).isEmpty()) {
                uri = URI.createURI(Path.of(string).toAbsolutePath().toUri().toString());
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Argument '%s' is not a valid file URI or path", string), e);
        }
    }

    protected abstract Predicate<URI> getFilter();

    protected Stream<Resource> collectResourcesAsStream(ResourceSet resourceSet) {
        return paths.stream()
            .flatMap(p -> URIAccessor.Registry.get(p)
                .orElseThrow(() -> new IllegalArgumentException("No handler for URI: " + p))
                .getContainedFiles(p, getFilter())
                .stream())
            .map(uri -> resourceSet.getResource(uri, true));
    }

}
