package tools.vitruv.neojoin.parse;

import org.eclipse.xtext.validation.Issue;
import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractIntegrationTest;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.utils.Pair;

import java.util.List;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class ImportParseTest extends AbstractIntegrationTest {

    @Override
    protected List<String> getMetaModelPaths() {
        return List.of("/models/restaurant.ecore", "/models/reviewpage.ecore");
    }

    private Pair<ViewTypeDefinition, List<Issue>> parse(String query) {
        return internalParse(query);
    }

    @Test
    void noImport() {
        var result = parse("""
            export something to "http://example.com"
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void importSingle() {
        var result = parse("""
            export something to "http://example.com"
            
            import "http://example.org/restaurant"
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void importMulti() {
        var result = parse("""
            export something to "http://example.com"
            
            import "http://example.org/restaurant"
            import "http://example.org/reviewpage"
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void importAlias() {
        var result = parse("""
            export something to "http://example.com"
            
            import "http://example.org/restaurant" as rest
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void importMultiAlias() {
        var result = parse("""
            export something to "http://example.com"
            
            import "http://example.org/restaurant" as rest
            import "http://example.org/reviewpage"
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void duplicatedUri() {
        var result = parse("""
            export something to "http://example.com"
            
            import "http://example.org/restaurant" as rest
            import "http://example.org/restaurant"
            """);

        assertThat(result).hasIssues("Duplicated import URI");
    }

    @Test
    void duplicatedPackage() {
        var result = parse("""
            export something to "http://example.com"
            
            import "http://example.org/restaurant"
            import "http://example.org/reviewpage" as restaurant
            """);

        assertThat(result).hasIssues("Duplicated import named 'restaurant'");
    }

    @Test
    void duplicatedPackageAlias() {
        var result = parse("""
            export something to "http://example.com"
            
            import "http://example.org/restaurant" as test
            import "http://example.org/reviewpage" as test
            """);

        assertThat(result).hasIssues("Duplicated import named 'test'");
    }

    @Test
    void importAliasEcore() {
        var result = parse("""
            export something to "http://example.com"
            
            import "http://example.org/restaurant" as ecore
            """);

        assertThat(result).hasIssues("Import alias 'ecore' is reserved for the Ecore package");
    }

}
