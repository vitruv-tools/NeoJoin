package tools.vitruv.neojoin.collector;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.vitruv.neojoin.common.EMFUtils;
import tools.vitruv.neojoin.common.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Searches for meta-model files with the {@code .ecore} extension and collects them into a package registry.
 * <p>
 * Files that cannot be loaded or contain no package, meta-models whose namespace URI several files define, and
 * meta-models that the source model inference cannot map (see {@link UnmappableMetaModels}) are reported and left
 * out. A meta-model whose namespace URI is registered globally (for example a copy of Ecore) is reported and replaced
 * by the global package.
 *
 * @see #collect()
 */
public class PackageModelCollector extends AbstractModelCollector {

    public static final String FileExtension = "ecore";

    @Override
    protected String fileExtension() {
        return FileExtension;
    }

    public PackageModelCollector(String searchPathString) {
        super(searchPathString);
    }

    public Pair<List<CollectorIssue>, EPackage.Registry> collect() {
        var state = new CollectionState();
        for (var result : loadResources(state.resourceSet, state.issues)) {
            if (result instanceof LoadResult.Loaded loaded) {
                state.add(loaded);
            } else if (result instanceof LoadResult.Failed failed) {
                state.issues.add(loadFailure(failed));
            }
        }
        state.register();
        state.issues.addAll(UnmappableMetaModels.remove(state.packages, state.registry, state.ambiguous));
        return new Pair<>(state.issues, state.registry);
    }

    /**
     * The state of one {@link #collect()}.
     */
    private static final class CollectionState {

        private final EPackage.Registry registry = new EPackageRegistryImpl();

        /**
         * The packages of the registry in the order in which they were collected, without global packages.
         */
        private final List<EPackage> packages = new ArrayList<>();

        /**
         * The packages whose namespace URI several files define.
         */
        private final Set<EPackage> ambiguous = new HashSet<>();

        private final List<CollectorIssue> issues = new ArrayList<>();

        /**
         * The loaded packages by namespace URI, in the order of loading.
         */
        private final Map<String, List<EPackage>> loaded = new LinkedHashMap<>();

        /**
         * References between the meta-models by namespace URI ({@code http://example.org/x#//Food}) are resolved
         * lazily through the package registry of this resource set.
         */
        private final ResourceSet resourceSet = EMFUtils.createResourceSet();

        void add(LoadResult.Loaded result) {
            var rootPackages = rootPackages(result.resource());
            if (rootPackages.isEmpty()) {
                issues.add(new CollectorIssue.LoadFailure(result.uri().toString(), "the file contains no package."));
            }
            rootPackages.stream()
                .flatMap(EMFUtils::getAllEPackages)
                .forEach(pack -> add(pack, result.uri().toString()));
        }

        private void add(EPackage pack, String file) {
            var namespaceUri = pack.getNsURI();
            var globalPackage = EPackage.Registry.INSTANCE.getEPackage(namespaceUri);
            if (globalPackage != null) {
                issues.add(new CollectorIssue.GlobalPackageConflict(namespaceUri, file));
                registry.put(namespaceUri, globalPackage);
            } else {
                loaded.computeIfAbsent(namespaceUri, uri -> new ArrayList<>()).add(pack);
            }
        }

        /**
         * Registers the loaded packages with a unique namespace URI. Of the others, the first one stays in the
         * resource set, so that references to it resolve and the referencing meta-models are left out too.
         */
        void register() {
            loaded.forEach((namespaceUri, candidates) -> {
                resourceSet.getPackageRegistry().put(namespaceUri, candidates.get(0));
                if (candidates.size() == 1) {
                    registry.put(namespaceUri, candidates.get(0));
                    packages.add(candidates.get(0));
                } else {
                    issues.add(new CollectorIssue.DuplicateNamespaceUri(
                        namespaceUri,
                        candidates.stream().map(pack -> pack.eResource().getURI().toString()).toList()
                    ));
                    ambiguous.addAll(candidates);
                }
            });
        }

        private static List<EPackage> rootPackages(Resource resource) {
            return resource.getContents().stream()
                .filter(EPackage.class::isInstance)
                .map(EPackage.class::cast)
                .toList();
        }
    }
}
