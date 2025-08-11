package tools.vitruv.neojoin.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.ValueSource;

import picocli.CommandLine;

public class IntegrationTest {

    static List<String> validQueries = List.of("actor-rating", "books-on-tape", "customer-borrowings", "movies");

    private URL getResource(String name) {
        return getClass().getClassLoader().getResource(name);
    }

    private Comparison compareEcoreFiles(Path modelA, Path modelB) {
        ResourceSet resourceSetA = new ResourceSetImpl();
        resourceSetA.getResource(URI.createURI(modelA.toUri().toString()), true);

        ResourceSet resourceSetB = new ResourceSetImpl();
        resourceSetB.getResource(URI.createURI(modelB.toUri().toString()), true);

        IComparisonScope scope = new DefaultComparisonScope(resourceSetA, resourceSetB, null);
        return EMFCompare.builder().build().compare(scope);
    }

    @ParameterizedTest
    @FieldSource("validQueries")
    public void testCheckQuery(String queryName) throws URISyntaxException {
        // GIVEN meta-models and a valid query on them
        URL metaModelPath = getResource("models");
        URL query = getResource("queries/" + queryName + ".nj");

        String metaModelPathArg = "--meta-model-path=" + metaModelPath.toURI();
        String queryArg = query.getPath();

        // WHEN checking the query
        int exitCode = new CommandLine(new Main()).execute(new String[] { metaModelPathArg, queryArg });
        
        // THEN the query is accepted
        assertEquals(0, exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = { "invalid" })
    public void testCheckQueryFail(String queryName) throws URISyntaxException {
        // GIVEN meta-models and an invalid query
        URL metaModelPath = getResource("models");
        URL query = getResource("queries/" + queryName + ".nj");

        String metaModelPathArg = "--meta-model-path=" + metaModelPath.toURI();
        String queryArg = query.getPath();

        // WHEN checking the query
        int exitCode = new CommandLine(new Main()).execute(new String[] { metaModelPathArg, queryArg });
        
        // THEN the query is rejected
        assertEquals(1, exitCode);
    }

    @ParameterizedTest
    @FieldSource("validQueries")
    public void testGenerateMetaModel(String queryName, @TempDir Path outputDirectory) throws URISyntaxException, IOException {
        // GIVEN meta-models, a valid query and an output path
        URL metaModelPath = getResource("models");
        URL query = getResource("queries/" + queryName + ".nj");

        Path output = outputDirectory.resolve(queryName + ".ecore");

        String metaModelPathArg = "--meta-model-path=" + metaModelPath.toURI();
        String generateArg = "--generate=" + output;
        String queryArg = query.getPath();

        // WHEN generating the view type
        int exitCode = new CommandLine(new Main()).execute(new String[] { metaModelPathArg, generateArg, queryArg });
        
        // THEN the correct view type is generated (ignoring the order of view type elements)
        URL result = getResource("results/" + queryName + ".ecore");

        Stream<Diff> differences = compareEcoreFiles(Path.of(result.toURI()), output).getDifferences().stream()
            .filter(diff -> diff.getKind() != DifferenceKind.MOVE);

        assertEquals(0, exitCode);
        assertTrue(differences.findAny().isEmpty());
    }

}
