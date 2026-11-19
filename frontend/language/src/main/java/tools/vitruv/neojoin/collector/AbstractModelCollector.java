package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.util.List;

/**
 * Collects models from a {@link ModelPath model path}. Supports both {@link PackageModelCollector meta-models} and
 * {@link InstanceModelCollector instance-models}. All files on the path with the file extension of the collector are
 * considered models and are loaded.
 */
public abstract class AbstractModelCollector {

    private final ModelPath path;

    private final String fileExtension;

    /**
     * @param searchPathString the model path, a semicolon separated list of model files, directories and archives
     * @param fileExtension    file extension of the models
     */
    protected AbstractModelCollector(String searchPathString, String fileExtension) {
        this.path = new ModelPath(searchPathString);
        this.fileExtension = fileExtension;
    }

    /**
     * Loads the models on the path into the given resource set, in the order of {@link ModelPath#findFiles}.
     *
     * @param resourceSet resource set to load the models into
     * @param issues      list to add the problems of the path to
     * @return the result of each file, in the order of loading
     */
    List<LoadResult> loadResources(ResourceSet resourceSet, List<CollectorIssue> issues) {
        return path.findFiles(fileExtension, issues).stream()
            .map(uri -> load(resourceSet, uri))
            .toList();
    }

    private static LoadResult load(ResourceSet resourceSet, URI uri) {
        try {
            return new LoadResult.Loaded(uri, resourceSet.getResource(uri, true));
        } catch (RuntimeException e) {
            return new LoadResult.Failed(uri, e);
        }
    }

}
