package tools.vitruv.neojoin.ide.integration;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Go to definition on elements of source meta-models opens the {@code .ecore} file at the start tag of the element.
 * Runs against the shaded jar like {@link NeoJoinIdeServerIT}.
 */
class SourceModelDefinitionIT {

    @TempDir
    static Path directory;

    private static InProcessLanguageServer server;
    private static String queryUri;
    private static String restaurantUri;
    private static QueryText query;
    private static QueryText restaurant;
    private static QueryText animals;
    private static QueryText dogs;

    /**
     * The query and the meta-models are in {@code src/test/resources/definition}, except the restaurant model, which
     * is shared (see {@code test-resources}). The test reads them from the class path and writes them into the
     * directory of the server.
     */
    @BeforeAll
    static void startServer() throws IOException, ExecutionException, InterruptedException {
        restaurant = copyToDirectory("/models/restaurant.ecore", "restaurant.ecore");
        animals = copyToDirectory("/definition/animals.ecore", "animals.ecore");
        dogs = copyToDirectory("/definition/dogs.ecore", "dogs.ecore");
        restaurantUri = uriOf("restaurant.ecore");
        server = InProcessLanguageServer.start(directory);

        try (InputStream stream = SourceModelDefinitionIT.class.getResourceAsStream("/definition/query.nj")) {
            query = new QueryText(new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8));
        }
        queryUri = server.open(directory.resolve("query.nj"), query.text());
    }

    private static QueryText copyToDirectory(String resource, String fileName) throws IOException {
        try (InputStream stream = SourceModelDefinitionIT.class.getResourceAsStream(resource)) {
            var text = new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8);
            Files.writeString(directory.resolve(fileName), text);
            return new QueryText(text);
        }
    }

    private static String uriOf(String fileName) {
        return directory.resolve(fileName).toUri().toString();
    }

    /**
     * Range of the start tag of the feature {@code name} of {@code Restaurant} (other classes in the file have a
     * {@code name} too).
     */
    private static Range restaurantNameRange() {
        return restaurant.rangeOfStartTagContaining("name=\"name\" lowerBound=\"1\"");
    }

    private static void assertDefinition(Position position, String uri, Range range) throws ExecutionException,
        InterruptedException {
        assertThat(server.definition(queryUri, position)).containsExactly(new Location(uri, range));
    }

    /**
     * While a user types a join, the class of the join is missing, so the feature does not resolve: no definition, no
     * failure.
     */
    @Test
    void featureInIncompleteJoinConditionHasNoDefinition() throws IOException, ExecutionException,
        InterruptedException {
        var incomplete = new QueryText("""
            import "http://example.org/restaurant"

            from Restaurant r
            join
                with r using name
            create Place {}
            """);
        var uri = server.open(directory.resolve("incomplete.nj"), incomplete.text());

        assertThat(server.definition(uri, incomplete.positionAtEndOf("using name"))).isEmpty();
    }

    /**
     * The other tests rely on a query that resolves all names.
     */
    @Test
    void queryHasNoErrors() throws ExecutionException, InterruptedException, TimeoutException {
        assertThat(server.diagnostics(queryUri))
            .extracting(Diagnostic::getSeverity)
            .doesNotContain(DiagnosticSeverity.Error);
    }

    @Test
    void definitionOfFromClassIsItsStartTagInTheEcoreFile() throws ExecutionException, InterruptedException {
        assertDefinition(
            query.positionAtEndOf("from Restaurant"), restaurantUri,
            restaurant.rangeOfStartTagContaining("name=\"Restaurant\"")
        );
    }

    @Test
    void definitionOfJoinClassIsItsStartTagInTheEcoreFile() throws ExecutionException, InterruptedException {
        assertDefinition(
            query.positionAtEndOf("join Food"), restaurantUri,
            restaurant.rangeOfStartTagContaining("name=\"Food\"")
        );
    }

    @Test
    void definitionOfImportIsTheStartTagOfThePackageInTheEcoreFile() throws ExecutionException, InterruptedException {
        assertDefinition(
            query.positionAtEndOf("import \"http://example.org/dogs"), uriOf("dogs.ecore"),
            dogs.rangeOfStartTagContaining("name=\"dogs\"")
        );
    }

    @Test
    void definitionOfDeclaredEnumTypeIsItsStartTagInTheEcoreFile() throws ExecutionException, InterruptedException {
        assertDefinition(
            query.positionAtEndOf("kind : FoodType"), restaurantUri,
            restaurant.rangeOfStartTagContaining("EEnum\" name=\"FoodType\"")
        );
    }

    @Test
    void definitionOfDeclaredDataTypeIsItsStartTagInTheEcoreFile() throws ExecutionException, InterruptedException {
        assertDefinition(
            query.positionAtEndOf("revenue : Money"), restaurantUri,
            restaurant.rangeOfStartTagContaining("name=\"Money\"")
        );
    }

    /**
     * Ecore's built-in types come from the registered package: there is no file that an editor could open.
     */
    @Test
    void builtInDataTypeHasNoDefinition() throws ExecutionException, InterruptedException {
        assertThat(server.definition(queryUri, query.positionAtEndOf("label : EString"))).isEmpty();
    }

    /**
     * A feature in a join condition belongs to both joined classes ({@code r} and {@code f}): the definitions are the
     * features of both.
     */
    @Test
    void definitionOfFeatureInJoinConditionAreTheFeaturesOfBothClasses()
        throws ExecutionException, InterruptedException {
        var definitions = server.definition(queryUri, query.positionAtEndOf("with r using name"));

        assertThat(definitions).containsExactly(
            new Location(restaurantUri, restaurantNameRange()),
            new Location(restaurantUri, restaurant.rangeOfStartTagContaining("name=\"name\" eType"))
        );
    }

    @Test
    void definitionOfSourceModelFeatureIsItsStartTagInTheEcoreFile() throws ExecutionException, InterruptedException {
        assertDefinition(
            query.positionAtEndOf("label : EString = r.name"), restaurantUri,
            restaurantNameRange()
        );
    }

    @Test
    void definitionOfSourceModelReferenceIsItsStartTagInTheEcoreFile() throws ExecutionException, InterruptedException {
        assertDefinition(
            query.positionAtEndOf("sold = r.sells"), restaurantUri,
            restaurant.rangeOfStartTagContaining("name=\"sells\"")
        );
    }

    /**
     * The inferred class copies the inherited features, but the definition is the declaring feature, in the file of the
     * super class.
     */
    @Test
    void definitionOfInheritedFeatureIsTheFeatureInTheFileOfTheSuperClass()
        throws ExecutionException, InterruptedException {
        assertDefinition(
            query.positionAtEndOf("legs := d.legs"), uriOf("animals.ecore"),
            animals.rangeOfStartTagContaining("name=\"legs\"")
        );
    }

    /**
     * {@code Dog} declares {@code name} again, which is invalid Ecore. EMF and the expression type resolve the name to
     * the inherited feature, so the definition is the feature of {@code Animal}.
     */
    @Test
    void definitionOfRedeclaredFeatureIsTheInheritedFeature() throws ExecutionException, InterruptedException {
        assertDefinition(
            query.positionAtEndOf("nickname := d.name"), uriOf("animals.ecore"),
            animals.rangeOfStartTagContaining("name=\"name\"")
        );
    }

    /**
     * The alias declares a name for the package like the alias of a source declares one for its class, so the
     * definition is the declaration in the query.
     */
    @Test
    void definitionOfImportAliasIsItsDeclaration() throws IOException, ExecutionException, InterruptedException {
        var aliased = new QueryText("""
            import "http://example.org/dogs" as pets

            from Dog d create Pet {}
            """);
        var uri = server.open(directory.resolve("aliased.nj"), aliased.text());

        assertThat(server.definition(uri, aliased.positionAtEndOf("as pets")))
            .extracting(Location::getUri)
            .containsExactly(uri);
        assertThat(server.definition(uri, aliased.positionAfter("as pets")))
            .extracting(Location::getUri)
            .containsExactly(uri);
    }

    /**
     * The cursor is right after the name (the editor highlights it there), at the end of a line or before an operator.
     */
    @Test
    void definitionOfFeatureWithCursorRightAfterItsNameIsFound() throws IOException, ExecutionException,
        InterruptedException {
        var touching = new QueryText("""
            import "http://example.org/dogs"

            from Dog dog
            create Dog {
                dog.name
                dog.legs
                nickname := dog.name + "_xy"
            }
            """);
        var uri = server.open(directory.resolve("touching.nj"), touching.text());
        var expected = new Location(uriOf("animals.ecore"), animals.rangeOfStartTagContaining("name=\"name\""));

        assertThat(server.definition(uri, touching.positionAfter("    dog.name"))).containsExactly(expected);
        assertThat(server.definition(uri, touching.positionAfter("dog.name"))).containsExactly(expected);
    }

    /**
     * The cursor is at the start of {@code length}, a member of a Java type without a definition. The name in front of
     * the dot only gets the definition when the cursor touches its end.
     */
    @Test
    void javaMemberAfterSourceModelFeatureHasNoDefinitionAtItsStart() throws IOException, ExecutionException,
        InterruptedException {
        var member = new QueryText("""
            import "http://example.org/dogs"

            from Dog dog
            create Dog {
                size := dog.name.length
            }
            """);
        var uri = server.open(directory.resolve("member.nj"), member.text());

        assertThat(server.definition(uri, member.positionAfter("dog.name."))).isEmpty();
        assertThat(server.definition(uri, member.positionAfter("dog.name"))).hasSize(1);
    }

    /**
     * Files checked out on Windows have CRLF line breaks: the cursor at the end of a line is in front of the CR.
     */
    @Test
    void definitionWithCursorRightAfterItsNameIsFoundWithWindowsLineBreaks()
        throws IOException, ExecutionException, InterruptedException {
        var text = new QueryText("""
            import "http://example.org/dogs"

            from Dog dog
            create Dog {
                dog.name
                dog.legs
            }
            """.replace("\n", "\r\n"));
        var uri = server.open(directory.resolve("crlf.nj"), text.text());
        var expected = new Location(uriOf("animals.ecore"), animals.rangeOfStartTagContaining("name=\"name\""));

        assertThat(server.definition(uri, text.positionAfter("dog.name"))).containsExactly(expected);
    }

    @Test
    void definitionOfFeatureOfSubClassIsItsStartTagInTheEcoreFile() throws ExecutionException, InterruptedException {
        assertDefinition(
            query.positionAtEndOf("breed := d.breed"), uriOf("dogs.ecore"),
            dogs.rangeOfStartTagContaining("name=\"breed\"")
        );
    }

}
