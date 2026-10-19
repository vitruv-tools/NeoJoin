package tools.vitruv.neojoin.cli.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static tools.vitruv.neojoin.cli.integration.Utils.getResource;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Disabled;
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
    @Disabled("Disabled until the parsing of the project works")
    public void testGenerateTripleGraphGrammar(@TempDir Path outputDirectory) throws IOException {
        // GIVEN meta-models, a valid query and a TGG project output path
        var metaModelPath = getResource(Utils.MODELS);
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
        final String tripleGraphGrammarContent = Files.readString(tripleGraphGrammarRulesFilePath);
        Files.writeString(
                tripleGraphGrammarRulesFilePath,
                tripleGraphGrammarContent + sourceMetamodel + targetMetamodel);

        // THEN a TGG project is generated
        assertEquals(0, exitCode);

        // TODO: Fix parsing
        final List<EObject> parsedObjects =
                EmslParser.parse(tripleGraphGrammarRulesFilePath.toAbsolutePath().toString());
        assertThat(parsedObjects).isNotNull().isNotEmpty();
    }
}
