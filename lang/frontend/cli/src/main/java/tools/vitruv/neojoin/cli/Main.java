package tools.vitruv.neojoin.cli;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
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
import tools.vitruv.neojoin.collector.InstanceModelCollector;
import tools.vitruv.neojoin.collector.PackageModelCollector;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.ManualPatternMatchingStrategy;
import tools.vitruv.neojoin.generation.MetaModelGenerator;
import tools.vitruv.neojoin.generation.ModelInfo;
import tools.vitruv.neojoin.tgg.emsl_utils.EmslMetamodelGenerator;
import tools.vitruv.neojoin.tgg.emsl_utils.EmslModelGenerator;
import tools.vitruv.neojoin.tgg.emsl_utils.EmslUtils;
import tools.vitruv.neojoin.transformation.Transformator;
import tools.vitruv.neojoin.transformation.TransformatorException;
import tools.vitruv.neojoin.utils.EMFUtils;
import tools.vitruv.neojoin.utils.Utils;
import tools.vitruv.optggs.driver.API;
import tools.vitruv.optggs.driver.Metamodel;
import tools.vitruv.optggs.driver.Model;
import tools.vitruv.optggs.driver.Project;
import tools.vitruv.optggs.operators.View;
import tools.vitruv.optggs.operators.ViewExtractor;
import tools.vitruv.optggs.transpiler.LocalNameResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    }

    @ArgGroup(exclusive = false, heading = "Generate Triple Graph Grammar (TGG) rules:%n")
    @Nullable
    TGGRuleGeneration tggRuleGeneration;

    static class TGGRuleGeneration {

        @Option(names = {"-tgg", "--generate-tgg-rules"}, paramLabel = "GENERATE TGG RULES", required = true, description = "Generate the corresponding TGG rules and write them to the given output directory")
        Path output;

        @Option(names = {"--source-model-path"}, paramLabel = "SOURCE_MODEL_PATH", description = "Optional: Path to the source model for the TGG transformation")
        Path sourceModelPath;

        @Option(names = {"--target-model-path"}, paramLabel = "TARGET_MODEL_PATH", description = "Optional: Path to the target model for the TGG transformation")
        Path targetModelPath;
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
        } catch (UnsupportedReferenceExpressionException e) {
            printError("Unsupported expression detected: %s", e.getMessage());
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
    private int execute() throws IOException, UnsupportedReferenceExpressionException {
        // collect available meta-models
        EPackage.Registry registry = new PackageModelCollector(metaModelPath).collect();
        ResourceSet sourceMetaModelResourceSet = new ResourceSetImpl();
        registry.values().stream()
            .filter(EPackage.class::isInstance)
            .map(EPackage.class::cast)
            .map(EPackage::eResource)
            .forEach(sourceMetaModelResourceSet.getResources()::add);

        // parse query
        var setup = new NeoJoinStandaloneSetup(registry);
        var result = setup.getParser().parse(URI.createFileURI(queryFile.toString()));

        printIssues(result.issues());

        if (result instanceof Parser.Result.Failure) {
            return 1;
        }

        if (generate == null && transform == null && tggRuleGeneration == null) {
            // neither generation nor transformation requested
            System.out.println("Query is valid. Use additional commands for further processing.");
            return 0;
        }

        var aqr = ((Parser.Result.Success) result).aqr();

        // generate meta-model
        var targetMetaModel = new MetaModelGenerator(aqr).generate();
        printIssues(targetMetaModel.diagnostic());
        if (generate != null) {
            EPackage targetMetamodelPackage = targetMetaModel.pack();
            EMFUtils.save(getOutputURI(generate.output, "ecore"), targetMetamodelPackage);
            registry.put(targetMetamodelPackage.getNsURI(), targetMetamodelPackage);
        }

        if (transform != null) {
            // transform instance models
            var inputModels = new InstanceModelCollector(transform.instanceModelPath, registry).collect();
            var targetInstanceModel = new Transformator(
                setup.getExpressionHelper(),
                aqr,
                targetMetaModel.pack(),
                inputModels
            ).transform();
            EMFUtils.save(getOutputURI(transform.output, "xmi"), targetInstanceModel);
            validateInstanceModel(targetInstanceModel);

        }

        if (tggRuleGeneration != null) {
            if (generate == null) {
                printError(
                        "TGG rule generation requires executing the Metamodel generation (--generate) too");
                return 1;
            }

            generateTripleGraphGrammarProject(sourceMetaModelResourceSet, targetMetaModel, aqr, registry);
        }

        return 0;
    }

    private void generateTripleGraphGrammarProject(
            ResourceSet sourceMetaModelResourceSet,
            ModelInfo targetMetaModel,
            AQR aqr,
            EPackage.Registry registry)
            throws UnsupportedReferenceExpressionException {
        if (tggRuleGeneration == null) {
            return;
        }

        // Generate metamodel(s) for eMoflon
        final Path sourceMetamodelPath = Path.of("target/emsl-source-metamodel.msl");
        EmslMetamodelGenerator.generateMetamodels(sourceMetaModelResourceSet, sourceMetamodelPath);

        final ResourceSet targetMetaModelResourceSet = new ResourceSetImpl();
        targetMetaModelResourceSet.getResources().add(targetMetaModel.pack().eResource());
        final Path targetMetamodelPath = Path.of("target/emsl-target-metamodel.msl");
        EmslMetamodelGenerator.generateMetamodels(targetMetaModelResourceSet, targetMetamodelPath);

        // TODO: How to choose Project name?
        final Project project = new Project("TestTGGProjectV2");
        project.addSourceMetamodel(new Metamodel(sourceMetamodelPath));
        project.addTargetMetamodel(new Metamodel(targetMetamodelPath));
        final View view = ViewExtractor.viewFromAQR(aqr, new ManualPatternMatchingStrategy());

        if (tggRuleGeneration.sourceModelPath != null) {
            final Path sourceModelOutputPath = Path.of("target/emsl-source-model.msl");
            final String sourceModelName =
                    convertEcoreModelToEmslAndGetModelName(
                            registry, tggRuleGeneration.sourceModelPath, sourceModelOutputPath);

            project.addSourceModel(new Model(sourceModelName, sourceModelOutputPath));
        }

        if (tggRuleGeneration.targetModelPath != null) {
            final Path targetModelOutputPath = Path.of("target/emsl-target-model.msl");
            final String targetModelName =
                    convertEcoreModelToEmslAndGetModelName(
                            registry, tggRuleGeneration.targetModelPath, targetModelOutputPath);

            project.addTargetModel(new Model(targetModelName, targetModelOutputPath));
        }

        API.generateProjectForView(
                project, view, tggRuleGeneration.output, new LocalNameResolver());
    }

    private static String convertEcoreModelToEmslAndGetModelName(
            EPackage.Registry registry, Path modelPath, Path outputPath) {
        ResourceSet modelResourceSet = new ResourceSetImpl();
        modelResourceSet.getPackageRegistry().putAll(registry);
        modelResourceSet
                .getResourceFactoryRegistry()
                .getExtensionToFactoryMap()
                .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
        Resource sourceModelResource =
                modelResourceSet.getResource(URI.createFileURI(modelPath.toString()), true);
        EcoreUtil.resolveAll(sourceModelResource);

        EmslModelGenerator.generateModels(modelResourceSet, outputPath);
        return EmslUtils.extractModelName(sourceModelResource.getURI());
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

        rootDiagnostic.getChildren().forEach(d -> {
            System.err.printf("[%s] %s%n", EMFUtils.diagnosticSeverityText(d), d.getMessage());
        });
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
