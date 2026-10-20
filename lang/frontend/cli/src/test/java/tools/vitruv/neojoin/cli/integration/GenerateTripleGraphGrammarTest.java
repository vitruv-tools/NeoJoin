package tools.vitruv.neojoin.cli.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static tools.vitruv.neojoin.cli.integration.Utils.getResource;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

import tools.vitruv.neojoin.cli.Main;
import tools.vitruv.neojoin.emsl_parser.EmslParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GenerateTripleGraphGrammarTest {

    /**
     * {@code java -jar cli.jar --meta-model-path=<meta-model-path> --generate=<metamodel-output>
     * --generate-tgg-rules=<tgg-project-output> <query>}
     */
    @Test
    public void testGenerateTripleGraphGrammar(
            @TempDir Path outputDirectory, @TempDir Path metaModelPath) throws IOException {
        // GIVEN meta-models, a valid query and a TGG project output path
        // Only the car meta-model is used, as other meta-models in the test resources define
        // classes with the same names, which cannot be resolved in a single TGG project
        Files.copy(
                getResource(Utils.MODELS.resolve("car.ecore")), metaModelPath.resolve("car.ecore"));
        var query = getResource(Utils.QUERIES.resolve("car.nj"));

        Path metaModelOutput = outputDirectory.resolve("metamodelOutput.ecore");
        final Path tggProjectOutput = outputDirectory.resolve("tgg-output");

        String metaModelPathArg = "--meta-model-path=" + metaModelPath;
        String generateArg = "--generate=" + metaModelOutput;
        String runTggTransformationArg = "--generate-tgg-rules=" + tggProjectOutput;
        String queryArg = query.toString();

        // WHEN generating the Triple Graph Grammar project
        int exitCode =
                new CommandLine(new Main())
                        .execute(metaModelPathArg, generateArg, runTggTransformationArg, queryArg);

        // We need to merge the metamodels into the TGG rule file, so that everything is resolved
        // correctly
        final Path tripleGraphGrammarRulesFilePath = tggProjectOutput.resolve("src/Transform.msl");
        final String sourceMetamodel =
                Files.readString(tggProjectOutput.resolve("src/SourceMetamodel.msl"));
        final String targetMetamodel =
                Files.readString(tggProjectOutput.resolve("src/TargetMetamodel.msl"));
        // The imports of the generated files use platform URIs, which cannot be resolved outside
        // of an Eclipse workspace
        final String tripleGraphGrammarContent =
                Files.readString(tripleGraphGrammarRulesFilePath)
                        .replaceAll("(?m)^import \"platform:/resource/.*\\R", "");
        Files.writeString(
                tripleGraphGrammarRulesFilePath,
                tripleGraphGrammarContent + sourceMetamodel + targetMetamodel);

        // THEN a TGG project is generated
        assertEquals(0, exitCode);

        final List<EObject> parsedObjects =
                EmslParser.parse(tripleGraphGrammarRulesFilePath.toAbsolutePath().toString());
        assertThat(parsedObjects).isNotNull().isNotEmpty();
    }
}
