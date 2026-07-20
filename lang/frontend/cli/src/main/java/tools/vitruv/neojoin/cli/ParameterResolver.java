package tools.vitruv.neojoin.cli;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.neojoin.aqr.AQRParameter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ParameterResolver {

    /**
     * Resolves raw CLI parameter strings into typed Java objects for use in query evaluation.
     *
     * @param aqrParams   declared parameters from the AQR
     * @param inputParams raw name=value pairs from the CLI
     * @param registry    package registry used to load XMI files for EClass parameters
     * @return map from parameter alias to typed value (null for optional parameters not provided)
     */
    static Map<String, Object> resolve(
        List<AQRParameter> aqrParams,
        Map<String, String> inputParams,
        EPackage.Registry registry
    ) {
        if (aqrParams.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> result = new HashMap<>();

        for (AQRParameter param : aqrParams) {
            if (!inputParams.containsKey(param.alias())) {
                result.put(param.alias(), null);
            } else {
                var rawValue = inputParams.get(param.alias());
                Object typedValue;
                if (param.type() instanceof EDataType dt) {
                    typedValue = getTypedParameter(rawValue, dt);
                } else if (param.type() instanceof EClass ec) {
                    if (param.isList()) {
                        typedValue = loadEClassListParameter(rawValue, ec, registry);
                    } else {
                        typedValue = loadEClassParameter(rawValue, ec, registry);
                    }
                } else {
                    throw new ParameterResolutionException(
                        "Unsupported parameter type '%s'".formatted(param.type().getName())
                    );
                }
                result.put(param.alias(), typedValue);
            }
        }

        return result;
    }

    private static Object getTypedParameter(String raw, EDataType type) {
        var cls = type.getInstanceClass();
        if (cls == String.class) return raw;
        try {
            if (cls == int.class || cls == Integer.class) return Integer.parseInt(raw);
            if (cls == double.class || cls == Double.class) return Double.parseDouble(raw);
            if (cls == boolean.class || cls == Boolean.class) return Boolean.parseBoolean(raw);
            if (cls == long.class || cls == Long.class) return Long.parseLong(raw);
            if (cls == float.class || cls == Float.class) return Float.parseFloat(raw);
        } catch (NumberFormatException e) {
            throw new ParameterResolutionException(
                "Invalid value '%s' for parameter of type '%s'".formatted(raw, type.getName()), e
            );
        }
        throw new ParameterResolutionException(
            "Unsupported parameter type '%s'".formatted(type.getName())
        );
    }

    private static EObject loadEClassParameter(String xmiPath, EClass expectedType, EPackage.Registry registry) {
        var resource = loadXmiResource(xmiPath, registry);

        if (resource.getContents().isEmpty()) {
            throw new ParameterResolutionException(
                "XMI file '%s' is empty (expected an instance of '%s')".formatted(
                    xmiPath, expectedType.getName())
            );
        }

        var obj = resource.getContents().get(0);
        if (!expectedType.isInstance(obj)) {
            throw new ParameterResolutionException(
                "XMI file '%s' contains an instance of '%s', expected '%s'".formatted(
                    xmiPath, obj.eClass().getName(), expectedType.getName())
            );
        }

        return obj;
    }

    private static List<EObject> loadEClassListParameter(String xmiPath, EClass expectedType, EPackage.Registry registry) {
        var resource = loadXmiResource(xmiPath, registry);

        var matches = resource.getContents().stream()
            .filter(obj -> expectedType.isInstance(obj))
            .toList();

        if (matches.isEmpty()) {
            throw new ParameterResolutionException(
                "XMI file '%s' contains no instances of '%s'".formatted(xmiPath, expectedType.getName())
            );
        }

        return matches;
    }

    private static Resource loadXmiResource(String xmiPath, EPackage.Registry registry) {
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("xmi")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("xmi", new XMIResourceFactoryImpl());
        }
        var resourceSet = new ResourceSetImpl();
        resourceSet.setPackageRegistry(registry);
        return resourceSet.getResource(URI.createFileURI(xmiPath), true);
    }
}
