package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * The result of loading one model file into a resource set.
 */
sealed interface LoadResult {

    URI uri();

    record Loaded(URI uri, Resource resource) implements LoadResult {}

    record Failed(URI uri, RuntimeException exception) implements LoadResult {

        /**
         * Returns the issue for the file.
         */
        CollectorIssue toIssue() {
            return CollectorIssue.LoadFailure.of(uri.toString(), exception);
        }
    }
}
