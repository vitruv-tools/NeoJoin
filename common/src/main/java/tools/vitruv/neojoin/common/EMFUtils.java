package tools.vitruv.neojoin.common;

import static tools.vitruv.neojoin.common.Assertions.check;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.ContentHandler;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.eclipse.emf.ecore.resource.impl.ExtensibleURIConverterImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.resource.impl.URIHandlerImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.jspecify.annotations.Nullable;

/**
 * Various utilities for EMF and especially Ecore models.
 */
public final class EMFUtils {

    private EMFUtils() {}

    /**
     * Returns a set of all {@link EPackage packages} contained in the given package registry.
     */
    public static Set<EPackage> collectAvailablePackages(EPackage.Registry registry) {
        var results = new HashSet<EPackage>();
        collectAvailablePackagesImpl(registry, results);
        return results;
    }

    /**
     * A package registry is a map from URI strings to EPackages. A package registry may also have a delegate registry
     * to which all {@link EPackage.Registry#getEPackage(String)} requests are forwarded if no corresponding package
     * is found within the registry itself. However, all methods inherited from the map super class only operate on the
     * directly contained packages and a package registry provides no functionality for retrieving all
     * (directly or indirectly) contained packages. The solution implemented here is to use Reflection to get the
     * contained
     * delegate registry.
     */
    private static void collectAvailablePackagesImpl(EPackage.Registry registry, Set<EPackage> results) {
        for (var uri : registry.keySet()) {
            results.add(registry.getEPackage(uri));
        }

        if (registry instanceof EPackageRegistryImpl) {
            try {
                var field = EPackageRegistryImpl.class.getDeclaredField("delegateRegistry");
                field.setAccessible(true);
                var delegateRegistry = (EPackage.Registry) field.get(registry);
                if (delegateRegistry != null) {
                    collectAvailablePackagesImpl(delegateRegistry, results);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Unexpected internal structure of package registry", e);
            }
        }
    }

    /**
     * Searches the super classes of the given class level by level (breadth first, nearest first) and returns the
     * elements that {@code lookup} finds for the classes of the nearest level where it finds any.
     *
     * @param clazz  class whose super classes are searched
     * @param lookup finds the elements that belong to a class (for example, the target classes of a source class)
     * @return the distinct elements of the nearest level in the order of discovery, empty if no super class has any
     */
    public static <T> List<T> findInNearestSuperClasses(
        EClass clazz,
        Function<EClass, ? extends Collection<T>> lookup
    ) {
        var visited = new HashSet<EClass>();
        visited.add(clazz);
        List<EClass> level = clazz.getESuperTypes();
        while (!level.isEmpty()) {
            var found = new LinkedHashSet<T>();
            var nextLevel = new ArrayList<EClass>();
            for (var superClass : level) {
                if (!visited.add(superClass)) {
                    continue;
                }
                found.addAll(lookup.apply(superClass));
                nextLevel.addAll(superClass.getESuperTypes());
            }
            if (!found.isEmpty()) {
                return List.copyOf(found);
            }
            level = nextLevel;
        }
        return List.of();
    }

    /**
     * Returns all instances of the given class contained in the given resource.
     */
    public static Stream<EObject> getAllInstances(Resource resource, EClass clazz) {
        return Utils.streamOf(resource.getAllContents()).filter(clazz::isInstance);
    }

    /**
     * Returns all direct instances of the given class contained in the given resource.
     */
    public static Stream<EObject> getAllDirectInstances(Resource resource, EClass clazz) {
        return Utils.streamOf(resource.getAllContents()).filter(c -> c.eClass().equals(clazz));
    }

    /**
     * Returns a stream with the given {@link EPackage package} and all recursively contained subpackages.
     *
     * @param pack package to search
     * @return stream with the given package and all subpackages (recursively)
     */
    public static Stream<EPackage> getAllEPackages(EPackage pack) {
        return Stream.concat(
            Stream.of(pack),
            pack.getESubpackages().stream()
                .flatMap(EMFUtils::getAllEPackages)
        );
    }

    /**
     * Returns a stream of all {@link EPackage packages} contained in the given resource including subpackages.
     *
     * @param resource resource to search
     * @return stream of all packages in the resource
     */
    public static Stream<EPackage> getAllEPackages(Resource resource) {
        if (resource.getContents().isEmpty()) {
            throw new NoSuchElementException();
        }
        return getAllEPackages((EPackage) resource.getContents().get(0));
    }

    /**
     * Returns a stream of all {@link EClassifier classifiers} contained in the given package and its subpackages
     * recursively.
     *
     * @param pack package to search
     * @return stream of all classifiers in the package and its subpackages
     */
    public static Stream<EClassifier> getAllEClassifiers(EPackage pack) {
        return getAllEPackages(pack)
            .flatMap(p -> p.getEClassifiers().stream());
    }

    /**
     * Returns all {@link EDataType data types} contained in the given package.
     */
    public static Stream<EDataType> getAllEDataTypes(EPackage pack) {
        return getAllEClassifiers(pack)
            .filter(EDataType.class::isInstance)
            .map(c -> (EDataType) c);
    }

    /**
     * Returns the fully-qualified name of the given package as a stream of package names from root to leaf.
     *
     * @param pack package to get the fully qualified name parts for
     * @return stream of fully qualified name parts
     */
    public static Stream<String> getFullyQualifiedNameAsStream(EPackage pack) {
        var parts = new ArrayList<String>();
        for (var current = pack; current != null; current = current.getESuperPackage()) {
            check(current.getName() != null, "package name must not be null");
            parts.add(current.getName());
        }

        return Utils.reversedStream(parts);
    }

    public static EPackage getRootPackage(EPackage pack) {
        var current = pack;
        while (current.getESuperPackage() != null) {
            current = current.getESuperPackage();
        }
        return current;
    }

    /**
     * Returns the text representation of the given {@link Diagnostic diagnostic's} severity.
     *
     * @param diagnostic the diagnostic to get the severity text for
     * @return text representation of the diagnostic severity
     */
    public static String diagnosticSeverityText(Diagnostic diagnostic) {
        if ((diagnostic.getSeverity() & Diagnostic.ERROR) != 0) {
            return "ERROR";
        } else if ((diagnostic.getSeverity() & Diagnostic.WARNING) != 0) {
            return "WARNING";
        } else if ((diagnostic.getSeverity() & Diagnostic.INFO) != 0) {
            return "INFO";
        } else {
            return "OK";
        }
    }

    /**
     * Creates a resource set for loading and saving models: it reads and writes {@code .ecore} and {@code .xmi} files,
     * and finds meta-models by namespace URI among the given packages, then in the global package registry (Ecore
     * itself and the generated packages on the class path). It does not load anything from the network: EMF
     * would otherwise download the namespace URI of an unknown meta-model ({@code http://example.org/x}) as its
     * location.
     *
     * @param metaModels packages to find by namespace URI (only the packages of the registry itself, not of its
     *                   delegate); they take precedence over global packages with the same namespace URI, so that
     *                   models are loaded with the packages that the queries use
     * @return a new resource set
     */
    public static ResourceSet createResourceSet(EPackage.Registry metaModels) {
        ResourceSet resourceSet = new ResourceSetImpl();

        var factories = resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap();
        factories.put("ecore", new EcoreResourceFactoryImpl());
        factories.put("xmi", new XMIResourceFactoryImpl());

        var packages = new EPackageRegistryImpl(EPackage.Registry.INSTANCE);
        packages.putAll(metaModels);
        resourceSet.setPackageRegistry(packages);

        List<URIHandler> uriHandlers = new ArrayList<>();
        uriHandlers.add(new NoNetworkURIHandler());
        uriHandlers.addAll(URIHandler.DEFAULT_HANDLERS);
        resourceSet.setURIConverter(
            new ExtensibleURIConverterImpl(uriHandlers, ContentHandler.Registry.INSTANCE.contentHandlers())
        );

        return resourceSet;
    }

    /**
     * Creates a resource set for loading and saving models without further meta-models.
     *
     * @see #createResourceSet(EPackage.Registry)
     */
    public static ResourceSet createResourceSet() {
        return createResourceSet(new EPackageRegistryImpl());
    }

    /**
     * Write the given {@link EObject} to a file specified by the given {@link URI} ({@code .ecore} or {@code .xmi}).
     *
     * @param uri    URI of the output file
     * @param object the {@link EObject} to write
     */
    public static void save(URI uri, EObject object) throws IOException {
        ResourceSet resourceSet = createResourceSet();

        Resource resource = resourceSet.createResource(uri);
        resource.getContents().add(object);

        Map<String, Object> options = Map.of(XMLResource.OPTION_URI_HANDLER, new RelativeURIResolver(resource));
        resource.save(options);
    }

    /**
     * Fails for {@code http} and {@code https} URIs instead of opening a network connection.
     */
    private static final class NoNetworkURIHandler extends URIHandlerImpl {

        @Override
        public boolean canHandle(URI uri) {
            return "http".equals(uri.scheme()) || "https".equals(uri.scheme());
        }

        @Override
        public InputStream createInputStream(URI uri, @Nullable Map<?, ?> options) throws IOException {
            throw new IOException("Models are not loaded from the network: " + uri);
        }

        @Override
        public OutputStream createOutputStream(URI uri, @Nullable Map<?, ?> options) throws IOException {
            throw new IOException("Models are not saved to the network: " + uri);
        }

        @Override
        public boolean exists(URI uri, @Nullable Map<?, ?> options) {
            return false;
        }
    }
}
