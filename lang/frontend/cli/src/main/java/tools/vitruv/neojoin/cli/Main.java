package tools.vitruv.neojoin.cli;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.xtext.validation.Issue;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import tools.vitruv.neojoin.NeoJoinStandaloneSetup;
import tools.vitruv.neojoin.Parser;
import tools.vitruv.neojoin.SourceLocation;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.aqr.AQRParameter;
import tools.vitruv.neojoin.collector.InstanceModelCollector;
import tools.vitruv.neojoin.collector.PackageModelCollector;
import tools.vitruv.neojoin.generation.MetaModelGenerator;
import tools.vitruv.neojoin.transformation.Transformator;
import tools.vitruv.neojoin.transformation.TransformatorException;
import tools.vitruv.neojoin.utils.EMFUtils;
import tools.vitruv.neojoin.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@NullUnmarked
@Command(
    name = "neojoin",
    version = "NeoJoin CLI 1.0.0",
    mixinStandardHelpOptions = true,
    footer = {
        "",
        "Model Path",
        "  A semicolon separated list of paths to search for models",
        "  used in the options --meta-model-path and --instance-model-path.",
        "  Examples:",
        "  - Linux: /path/to/directory;/path/to/file.ecore",
        "  - Windows: C:\\path\\to\\directory;C:/path/to/file.ecore",
    })
public class Main implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "QUERY", description = "Path to the query file.")
    Path queryFile;



    @Option(names = {"-m", "--meta-model-path"}, paramLabel = "MODEL-PATH", required = true, description = "Model path (see below) to find referenced meta-models (.ecore).")
    String metaModelPath;


    @ArgGroup(exclusive = false, heading = "Generate the meta-model:%n")
    @Nullable
    Generate generate;

    static class Generate {

        @Option(names = {"-g", "--generate"}, paramLabel = "OUTPUT", required = true, description = "Generate the meta-model and write it to the given output file or directory.")
        Path output;

    }


    @ArgGroup(exclusive = false, heading = "Transform the input models:%n")
    @Nullable
    Transform transform;

    static class Transform {

        @Option(names = {"-i", "--instance-model-path"}, paramLabel = "MODEL-PATH", required = true, description = "Model path (see below) to find instance models (.xmi).")
        String instanceModelPath;

        @Option(names = {"-t", "--transform"}, paramLabel = "OUTPUT", required = true, description = "Transform the input models based on the query and write the result to the given output file or directory.")
        Path output;

        @Option(names = {"-p", "--parameters"}, paramLabel = "PARAMS", split = ",", required = false,
                description = "Query parameters as comma-separated name=value pairs. " +
                              "For EClass/EList parameters the value is a path to an XMI file. " +
                              "Example: -p featureName=Navigation,activeFeatures=config.xmi")
        Map<String, String> parameters;

    }

    /**
     * CLI entry point.
     *
     * @return exit code
     */
    @Override
    public Integer call() {
        try {
            return execute();
        } catch (IllegalArgumentException ex) {
            printError("Invalid meta-model path: %s", ex.getMessage());
        } catch (TransformatorException e) {
            var source = e.getSourceLocation();
            if (source != null) {
                printError("%s (%s)", e.getMessage(), source.display());
            } else {
                printError(e.getMessage());
            }
        } catch (IOException e) {
            printError("Failed to write output: %s", e.getMessage());
        } catch (WrappedException e) {
            printError("Failed to read input model: %s", e.getMessage());
        }

        return 1;
    }

    /**
     * Prints an error message with optional formatting arguments.
     *
     * @param message message format string
     * @param args    formatting arguments
     * @see java.io.PrintStream#printf(String, Object...)
     */
    private void printError(String message, @Nullable Object... args) {
        System.err.print("[ERROR] ");
        System.err.printf(message, args);
        System.err.println();
    }

    /**
     * Execute the CLI operation.
     *
     * @return exit code
     */
    private int execute() throws IOException {
        // collect available meta-models
        EPackage.Registry registry = new PackageModelCollector(metaModelPath).collect();

        // parse query
        var setup = new NeoJoinStandaloneSetup(registry);
        var result = setup.getParser().parse(URI.createFileURI(queryFile.toString()));

        printIssues(result.issues());

        if (result instanceof Parser.Result.Failure) {
            return 1;
        }

        if (generate == null && transform == null) {
            // neither generation nor transformation requested
            System.out.println("Query is valid. Use --generate or --transform for further processing.");
            return 0;
        }

        var aqr = ((Parser.Result.Success) result).aqr();

        // generate meta-model
        var targetMetaModel = new MetaModelGenerator(aqr).generate();
        printIssues(targetMetaModel.diagnostic());
        if (generate != null) {
            EMFUtils.save(getOutputURI(generate.output, "ecore"), targetMetaModel.pack());
        }

        if (transform != null) {
            // transform instance models
            var inputModels = new InstanceModelCollector(transform.instanceModelPath, registry).collect();
            Map<String, Object> paramValues = resolveParamValues(aqr, registry);

            var targetInstanceModel = new Transformator(
                setup.getExpressionHelper(),
                aqr,
                targetMetaModel.pack(), 
                inputModels,
                paramValues
            ).transform();
            EMFUtils.save(getOutputURI(transform.output, "xmi"), targetInstanceModel);
            validateInstanceModel(targetInstanceModel);
        }

        return 0;
    }

    private Map<String, Object> resolveParamValues(AQR aqr, EPackage.Registry registry) throws IOException {
        var aqrParams = aqr.parameters();
        if (aqrParams.isEmpty()) {
            return Map.of();
        }

        Map<String, String> inputParams = transform.parameters != null ? transform.parameters : Map.of();

        HashMap<String, Object> result = new HashMap<>();
        for (AQRParameter param : aqrParams) {
            if (!inputParams.containsKey(param.alias())) {
                throw new IllegalArgumentException(
                    "Missing value for parameter '%s' of type '%s'.".formatted(param.alias(), param.type().getName())
                );
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
                    throw new IllegalArgumentException(
                        "Unsupported parameter type '%s'".formatted(param.type().getName())
                    );
                }
                result.put(param.alias(), typedValue);
            }
        }

        return result;
    }

    private Object getTypedParameter(String raw, EDataType type) {
        var cls = type.getInstanceClass();
        if (cls == String.class) return raw;
        if (cls == int.class || cls == Integer.class) return Integer.parseInt(raw);
        if (cls == double.class || cls == Double.class) return Double.parseDouble(raw);
        if (cls == boolean.class || cls == Boolean.class) return Boolean.parseBoolean(raw);
        if (cls == long.class || cls == Long.class)  return Long.parseLong(raw);
        if (cls == float.class || cls == Float.class) return Float.parseFloat(raw);
        throw new IllegalArgumentException(
            "Unsupported parameter type '%s'".formatted(type.getName())
        );
    }

    private EObject loadEClassParameter(String xmiPath, EClass expectedType, EPackage.Registry registry)
        throws IOException {
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("xmi")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("xmi", new XMIResourceFactoryImpl());
        }

        var resourceSet = new ResourceSetImpl();
        resourceSet.setPackageRegistry(registry);

        var resource = resourceSet.getResource(URI.createFileURI(xmiPath), true);

        if (resource.getContents().isEmpty()) {
            throw new IllegalArgumentException(
                "XMI file '%s' is empty (expected an instance of '%s')".formatted(
                    xmiPath, expectedType.getName())
            );
        }

        var obj = resource.getContents().get(0);
        if (!expectedType.isInstance(obj)) {
            throw new IllegalArgumentException(
                "XMI file '%s' contains an instance of '%s', expected '%s'".formatted(
                    xmiPath, obj.eClass().getName(), expectedType.getName())
            );
        }

        return obj;
    }

    private List<EObject> loadEClassListParameter(String xmiPath, EClass expectedType, EPackage.Registry registry)
        throws IOException {
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("xmi")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("xmi", new XMIResourceFactoryImpl());
        }

        var resourceSet = new ResourceSetImpl();
        resourceSet.setPackageRegistry(registry);

        var resource = resourceSet.getResource(URI.createFileURI(xmiPath), true);

        var matches = resource.getContents().stream()
            .filter(obj -> expectedType.isInstance(obj))
            .toList();

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                "XMI file '%s' contains no instances of '%s'".formatted(xmiPath, expectedType.getName())
            );
        }

        return matches;
    }


    private static void printIssues(List<Issue> issues) {
        for (Issue issue : issues) {
            System.err.printf(
                "[%s] %s (%s)%n",
                issue.getSeverity().name(),
                issue.getMessage(),
                SourceLocation.from(issue).display()
            );
        }
    }

    private static void printIssues(Diagnostic rootDiagnostic) {
        if (rootDiagnostic.getSeverity() == Diagnostic.OK) {
            return;
        }

        rootDiagnostic.getChildren().forEach(d ->
            System.err.printf("[%s] %s%n", EMFUtils.diagnosticSeverityText(d), d.getMessage())
        );
    }

    /**
     * Infer the output file URI based on the given output path and file extension.
     *
     * @param output    the output path (file or directory)
     * @param extension file extension of the output file
     * @return the output file URI
     */
    private URI getOutputURI(Path output, String extension) {
        if (Files.isDirectory(output)) { // if output is given as directory, infer file name from input file
            String outputFileName = Utils.removeSuffix(queryFile.getFileName().toString(), ".nj") + "." + extension;
            output = output.resolve(outputFileName);
        }

        return URI.createFileURI(output.toString());
    }

    /**
     * Validate the given instance model and print any validation issues.
     *
     * @param instanceModel the instance model to validate
     */
    private void validateInstanceModel(EObject instanceModel) {
        var rootDiagnostic = Diagnostician.INSTANCE.validate(instanceModel);
        if (rootDiagnostic.getSeverity() == Diagnostic.OK) {
            return;
        }

        System.err.println("[WARNING] Generated instance model failed validation:");
        for (var diagnostic : rootDiagnostic.getChildren()) {
            if (diagnostic.getSeverity() != Diagnostic.OK) {
                System.err.printf("[%s] %s%n", getSeverityString(diagnostic), diagnostic.getMessage());
            }
        }
    }

    private String getSeverityString(Diagnostic diagnostic) {
        return switch (diagnostic.getSeverity()) {
            case Diagnostic.OK -> "OK";
            case Diagnostic.INFO -> "INFO";
            case Diagnostic.WARNING -> "WARNING";
            case Diagnostic.ERROR -> "ERROR";
            default -> "UNKNOWN";
        };
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

}
