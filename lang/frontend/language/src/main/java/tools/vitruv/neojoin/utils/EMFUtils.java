package tools.vitruv.neojoin.utils;

import static tools.vitruv.neojoin.utils.Assertions.check;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;

/**
 * Various utilities for EMF and especially Ecore models.
 */
public final class EMFUtils {

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
     * (directly or indirectly) contained packages. The solution implemented here is to use Reflection to get the contained
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
     * Returns all instances of the given class contained in the given resource.
     */
    public static Stream<EObject> getAllInstances(Resource resource, EClass clazz) {
        return Utils.streamOf(resource.getAllContents()).filter(clazz::isInstance);
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
     * Returns a stream of all {@link EClassifier classifiers} contained in the given package and its subpackages recursively.
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
     * Write the given {@link EObject} to a file specified by the given {@link URI}. Requires a registered resource factory
     * for the file extension of the given {@link URI}.
     *
     * @param uri    URI of the output file
     * @param object the {@link EObject} to write
     */
    public static void save(URI uri, EObject object) throws IOException {
        ResourceSet resourceSet = new ResourceSetImpl();
        
        Resource resource = resourceSet.createResource(uri);
        resource.getContents().add(object);
        
        Map<String, Object> options = Map.of(XMLResource.OPTION_URI_HANDLER, new RelativeURIResolver(resource));
        resource.save(options);
    }

}
