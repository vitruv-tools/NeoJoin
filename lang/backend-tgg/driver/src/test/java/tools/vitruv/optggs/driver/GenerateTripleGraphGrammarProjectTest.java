package tools.vitruv.optggs.driver;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.vitruv.neojoin.NeoJoinStandaloneSetup;
import tools.vitruv.neojoin.Parser;
import tools.vitruv.neojoin.aqr.AQR;
import tools.vitruv.neojoin.collector.PackageModelCollector;
import tools.vitruv.neojoin.expression_parser.parser.exception.UnsupportedReferenceExpressionException;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.ManualPatternMatchingStrategy;
import tools.vitruv.neojoin.generation.MetaModelGenerator;
import tools.vitruv.neojoin.tgg.emsl_parser.EmslParser;
import tools.vitruv.neojoin.tgg.emsl_utils.EmslMetamodelGenerator;
import tools.vitruv.neojoin.utils.EMFUtils;
import tools.vitruv.optggs.operators.View;
import tools.vitruv.optggs.operators.ViewExtractor;
import tools.vitruv.optggs.transpiler.LocalNameResolver;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * End-to-end test of the TGG generation: A NeoJoin query on a source meta-model is parsed, the
 * target meta-model is generated, and a TGG project is generated for the extracted view. The
 * resulting triple graph grammar is then parsed and validated by the eMSL parser.
 */
class GenerateTripleGraphGrammarProjectTest {
    private static final String PROJECT_NAME = "TestTGGProject";

    @Test
    void generatedTripleGraphGrammarIsValid(
            @TempDir Path metaModelDirectory,
            @TempDir Path emslDirectory,
            @TempDir Path projectDirectory,
            @TempDir Path templateCacheDirectory)
            throws IOException, URISyntaxException, UnsupportedReferenceExpressionException {
        // GIVEN a source meta-model, a valid query and a TGG project output path
        // Only the car meta-model is used, as other meta-models might define classes with the same
        // names, which cannot be resolved in a single TGG project
        Files.copy(resource("models/car.ecore"), metaModelDirectory.resolve("car.ecore"));
        final Path query = resource("queries/car.nj");
        final EPackage.Registry registry =
                new PackageModelCollector(metaModelDirectory.toString()).collect().right();

        // WHEN generating the Triple Graph Grammar project
        final ResourceSet sourceMetaModelResourceSet = new ResourceSetImpl();
        registry.values().stream()
                .filter(EPackage.class::isInstance)
                .map(EPackage.class::cast)
                .map(EPackage::eResource)
                .forEach(sourceMetaModelResourceSet.getResources()::add);

        final var parseResult =
                new NeoJoinStandaloneSetup(registry)
                        .getParser()
                        .parse(URI.createFileURI(query.toString()));
        assertThat(parseResult).isInstanceOf(Parser.Result.Success.class);
        final AQR aqr = ((Parser.Result.Success) parseResult).aqr();

        final EPackage targetMetaModel = new MetaModelGenerator(aqr).generate().pack();
        // Saving the generated meta-model attaches it to a resource
        EMFUtils.save(
                URI.createFileURI(emslDirectory.resolve("target.ecore").toString()),
                targetMetaModel);
        final ResourceSet targetMetaModelResourceSet = new ResourceSetImpl();
        targetMetaModelResourceSet.getResources().add(targetMetaModel.eResource());

        final Path sourceMetamodelPath = emslDirectory.resolve("emsl-source-metamodel.msl");
        final Path targetMetamodelPath = emslDirectory.resolve("emsl-target-metamodel.msl");
        EmslMetamodelGenerator.generateMetamodels(sourceMetaModelResourceSet, sourceMetamodelPath);
        EmslMetamodelGenerator.generateMetamodels(targetMetaModelResourceSet, targetMetamodelPath);

        final Project project = new Project(PROJECT_NAME);
        project.addSourceMetamodel(new Metamodel(sourceMetamodelPath));
        project.addTargetMetamodel(new Metamodel(targetMetamodelPath));
        final View view = ViewExtractor.viewFromAQR(aqr, new ManualPatternMatchingStrategy());
        API.generateProjectForView(
                project, view, projectDirectory, new LocalNameResolver(), templateCacheDirectory);

        // THEN a TGG project is generated
        assertThat(projectDirectory.resolve(".project")).exists();
        assertThat(projectDirectory.resolve("META-INF/MANIFEST.MF")).exists();
        assertThat(projectDirectory.resolve("src/SourceMetamodel.msl")).exists();
        assertThat(projectDirectory.resolve("src/TargetMetamodel.msl")).exists();
        final Path tripleGraphGrammarFile = projectDirectory.resolve("src/Transform.msl");
        assertThat(tripleGraphGrammarFile).exists();

        // AND the triple graph grammar can be parsed and validated
        // The meta-models need to be merged into the rule file, so that everything is resolved
        // correctly. The imports of the generated files use platform URIs, which cannot be
        // resolved outside of an Eclipse workspace, so they are removed.
        final String tripleGraphGrammar =
                Files.readString(tripleGraphGrammarFile)
                        .replaceAll("(?m)^import \"platform:/resource/.*\\R", "");
        final String sourceMetamodel =
                Files.readString(projectDirectory.resolve("src/SourceMetamodel.msl"));
        final String targetMetamodel =
                Files.readString(projectDirectory.resolve("src/TargetMetamodel.msl"));
        Files.writeString(
                tripleGraphGrammarFile, tripleGraphGrammar + sourceMetamodel + targetMetamodel);

        final List<EObject> parsedObjects =
                EmslParser.parse(tripleGraphGrammarFile.toAbsolutePath().toString());
        assertThat(parsedObjects).isNotNull().isNotEmpty();
    }

    private static Path resource(String name) throws URISyntaxException {
        return Path.of(
                GenerateTripleGraphGrammarProjectTest.class
                        .getClassLoader()
                        .getResource(name)
                        .toURI());
    }
}
