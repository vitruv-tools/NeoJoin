package tools.vitruv.neojoin.ide.integration;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sends requests to the language server as an editor does. Runs against the shaded jar (see the failsafe configuration
 * in the POM), so it also detects classes that the jar lacks.
 */
class NeoJoinIdeServerIT {

    private static final QueryText QUERY = new QueryText("""
        export package to "http://example.com"

        import "http://example.org/restaurant"

        param prefix : EString

        from Restaurant r create Test {
            label := r.name.blank
            copy := r
            prefixed := prefix + r.name
            prefixLength := prefix.length
        }
        """);

    @TempDir
    static Path directory;

    private static InProcessLanguageServer server;
    private static String queryUri;

    @BeforeAll
    static void startServer() throws IOException, ExecutionException, InterruptedException {
        try (InputStream model = NeoJoinIdeServerIT.class.getResourceAsStream("/models/restaurant.ecore")) {
            Files.copy(Objects.requireNonNull(model), directory.resolve("restaurant.ecore"));
        }
        server = InProcessLanguageServer.start(directory);
        queryUri = server.open(directory.resolve("query.nj"), QUERY.text());
    }

    private static Range rangeOfVariableDeclaration() {
        return QUERY.rangeOfLastCharacterOf("from Restaurant r");
    }

    @Test
    void validQueryHasNoErrors() throws ExecutionException, InterruptedException, TimeoutException {
        assertThat(server.diagnostics(queryUri))
            .extracting(Diagnostic::getSeverity)
            .doesNotContain(DiagnosticSeverity.Error);
    }

    @Test
    void generatedViewTypeContainsTheQueriedClass() throws ExecutionException, InterruptedException {
        server.generateViewType(queryUri);

        var resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
            .put("ecore", new EcoreResourceFactoryImpl());
        var viewTypeUri = URI.createFileURI(directory.resolve("query.nj.ecore").toString());
        var viewType = (EPackage) resourceSet.getResource(viewTypeUri, true).getContents().get(0);
        var test = (EClass) viewType.getEClassifier("Test");
        assertThat(test.getEStructuralFeatures())
            .extracting(ENamedElement::getName)
            .containsExactly("label", "copy", "prefixed", "prefixLength");
    }

    @Test
    void definitionOfVariableIsItsDeclaration() throws ExecutionException, InterruptedException {
        var definitions = server.definition(queryUri, QUERY.positionAtEndOf("copy := r"));

        assertThat(definitions).containsExactly(new Location(queryUri, rangeOfVariableDeclaration()));
    }

    /**
     * Java types have no location that an editor can open.
     */
    @Test
    void javaMemberHasNoDefinition() throws ExecutionException, InterruptedException {
        assertThat(server.definition(queryUri, QUERY.positionAtEndOf("r.name.blank"))).isEmpty();
    }

    @Test
    void sourceModelFeatureHasNoDefinition() throws ExecutionException, InterruptedException {
        assertThat(server.definition(queryUri, QUERY.positionAtEndOf("label := r.name"))).isEmpty();
    }

    /**
     * Each feature expression is a method of its own with a parameter for the variable, so the uses refer to different
     * parameters.
     */
    @Test
    void referencesOfVariableFromAUseAreTheDeclarationAndAllUses() throws ExecutionException, InterruptedException {
        var references = server.references(queryUri, QUERY.positionAtEndOf("copy := r"));

        assertThat(references).extracting(Location::getRange).containsExactlyInAnyOrder(
            rangeOfVariableDeclaration(),
            QUERY.rangeOfLastCharacterOf("label := r"),
            QUERY.rangeOfLastCharacterOf("copy := r"),
            QUERY.rangeOfLastCharacterOf("prefix + r")
        );
    }

    @Test
    void referencesOfVariableFromItsDeclarationAreTheDeclarationAndAllUses()
        throws ExecutionException, InterruptedException {
        var references = server.references(queryUri, QUERY.positionAtEndOf("from Restaurant r"));

        assertThat(references).extracting(Location::getRange).containsExactlyInAnyOrder(
            rangeOfVariableDeclaration(),
            QUERY.rangeOfLastCharacterOf("label := r"),
            QUERY.rangeOfLastCharacterOf("copy := r"),
            QUERY.rangeOfLastCharacterOf("prefix + r")
        );
    }

    @Test
    void referencesOfQueryParameterFromItsDeclarationAreTheDeclarationAndAllUses()
        throws ExecutionException, InterruptedException {
        var references = server.references(queryUri, QUERY.positionAtEndOf("param prefix"));

        assertThat(references).extracting(Location::getRange).containsExactlyInAnyOrder(
            QUERY.rangeAtEndOf("param prefix", "prefix"),
            QUERY.rangeAtEndOf("prefixed := prefix", "prefix"),
            QUERY.rangeAtEndOf("prefixLength := prefix", "prefix")
        );
    }

}
