package tools.vitruv.neojoin.collector;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.common.EMFUtils;
import tools.vitruv.neojoin.common.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

/**
 * Searches for instance-model files with the {@code .xmi} extension and collects them into a map indexed by
 * the package that they instantiate.
 *
 * @see #collect()
 */
public class InstanceModelCollector extends AbstractModelCollector {

    public static final String FileExtension = "xmi";

    private static final String XMI_NAMESPACE = "http://www.omg.org/XMI";

    private final EPackage.Registry registry;

    public InstanceModelCollector(String searchPathString, EPackage.Registry registry) {
        super(searchPathString, FileExtension);
        this.registry = registry;
    }

    /**
     * Loads the instance models, which may use the meta-models of the registry and Ecore. The meta-models of a file are
     * the ones of its root elements (of the first one if the file cannot be loaded).
     *
     * @return the issues and the instance models by the meta-model they instantiate; instance models of other
     *     meta-models and files that cannot be loaded are reported and left out
     */
    public Pair<List<CollectorIssue>, Map<EPackage, Resource>> collect() {
        var state = new CollectionState(EMFUtils.collectAvailablePackages(registry));
        var resourceSet = EMFUtils.createResourceSet(registry);

        for (var result : loadResources(resourceSet, state.issues)) {
            if (result instanceof LoadResult.Loaded loaded) {
                state.add(loaded);
            } else if (result instanceof LoadResult.Failed failed) {
                var namespace = readRootNamespace(failed.uri(), resourceSet);
                state.add(failed, namespace, namespace != null ? registry.getEPackage(namespace) : null);
            }
        }

        return new Pair<>(state.issues, state.models);
    }

    /**
     * The state of one {@link #collect()}.
     */
    private static final class CollectionState {

        private final Set<EPackage> knownPackages;

        private final List<CollectorIssue> issues = new ArrayList<>();

        private final Map<EPackage, Resource> models = new HashMap<>();

        /**
         * The first file per meta-model, also if it cannot be loaded.
         */
        private final Map<EPackage, String> firstFiles = new HashMap<>();

        CollectionState(Set<EPackage> knownPackages) {
            this.knownPackages = knownPackages;
        }

        void add(LoadResult.Loaded loaded) {
            var contents = loaded.resource().getContents();
            if (contents.isEmpty()) {
                issues.add(new CollectorIssue.LoadFailure(
                    loaded.uri().toString(),
                    "the file contains no model elements."
                ));
                return;
            }

            var instancedPackages = contents.stream().map(root -> root.eClass().getEPackage()).distinct().toList();
            for (var instancedPackage : instancedPackages) {
                if (knownPackages.contains(instancedPackage)) {
                    recordInstanceModel(instancedPackage, loaded.uri());
                    models.putIfAbsent(instancedPackage, loaded.resource());
                } else {
                    issues.add(new CollectorIssue.MetaModelOutsideModelPath(
                        loaded.uri().toString(),
                        instancedPackage.getNsURI()
                    ));
                }
            }
        }

        /**
         * Reports a file that cannot be loaded. If its root element names a meta-model of the registry, the file counts
         * as instance model of that meta-model, because it may be the intended one.
         *
         * @param namespace        the namespace URI of the root element, if it can be read
         * @param instancedPackage the package of the registry with that namespace URI, if any
         */
        void add(LoadResult.Failed failed, @Nullable String namespace, @Nullable EPackage instancedPackage) {
            if (instancedPackage != null) {
                issues.add(failed.toIssue());
                recordInstanceModel(instancedPackage, failed.uri());
            } else if (namespace != null) {
                issues.add(new CollectorIssue.MetaModelOutsideModelPath(failed.uri().toString(), namespace));
            } else {
                issues.add(failed.toIssue());
            }
        }

        /**
         * Records the file as instance model of the package and reports an error if the package already has one.
         */
        private void recordInstanceModel(EPackage instancedPackage, URI file) {
            var previous = firstFiles.putIfAbsent(instancedPackage, file.toString());
            if (previous != null) {
                issues.add(new CollectorIssue.MultipleInstanceModels(
                    instancedPackage.getNsURI(),
                    previous,
                    file.toString()
                ));
            }
        }
    }

    /**
     * Returns the namespace URI of the root element of an XMI file (of the first element within {@code xmi:XMI}), or
     * {@code null} if the file cannot be read that far.
     */
    private static @Nullable String readRootNamespace(URI file, ResourceSet resourceSet) {
        var factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        try (var input = resourceSet.getURIConverter().createInputStream(file)) {
            var reader = factory.createXMLStreamReader(input);
            try {
                while (reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT
                        && !XMI_NAMESPACE.equals(reader.getNamespaceURI())) {
                        return reader.getNamespaceURI();
                    }
                }
                return null;
            } finally {
                reader.close();
            }
        } catch (IOException | XMLStreamException e) {
            return null;
        }
    }

}
