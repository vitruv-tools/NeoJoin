package tools.vitruv.neojoin.parse;

import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.validation.Issue;
import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractIntegrationTest;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.utils.Pair;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

class ConformsParseTest extends AbstractIntegrationTest {

    private static final String REF_URI = "http://example.org/foodref";

    @Override
    protected List<String> getMetaModelPaths() {
        return List.of("/models/restaurant.ecore", "/models/foodref.ecore");
    }

    private Pair<ViewTypeDefinition, List<Issue>> parse(String query) {
        return internalParse(query);
    }

    private static boolean hasSyntaxError(List<Issue> issues) {
        return issues.stream().anyMatch(i -> i.getSeverity() == Severity.ERROR
            && "org.eclipse.xtext.diagnostics.Diagnostic.Syntax".equals(i.getCode()));
    }

    @Test
    void noConforms() {
        var result = parse("""
            export something to "http://example.com"
            """);

        assertThat(result).hasNoIssues();
        assertThat(result.left().getConforms()).isNull();
    }

    @Test
    void conforms() {
        var result = parse("""
            export something to "http://example.com"
            conforms to "http://example.org/foodref"

            import "http://example.org/restaurant"

            from Food f
            create Food {
                f.name
            }
            """);

        assertThat(result).hasNoIssues();
        var conforms = result.left().getConforms();
        assertThat(conforms).isNotNull();
        assertThat(conforms.isStrict()).isFalse();
        assertThat(conforms.getUri()).isEqualTo(REF_URI);
    }

    @Test
    void strictConforms() {
        var result = parse("""
            export something to "http://example.com"
            strict conforms to "http://example.org/foodref"

            import "http://example.org/restaurant"

            from Food f
            create Food {
                f.name
            }
            """);

        assertThat(result).hasNoIssues();
        var conforms = result.left().getConforms();
        assertThat(conforms).isNotNull();
        assertThat(conforms.isStrict()).isTrue();
        assertThat(conforms.getUri()).isEqualTo(REF_URI);
    }

    @Test
    void conformsWithImportParamAndQuery() {
        var result = parse("""
            export something to "http://example.com"
            conforms to "http://example.org/foodref"

            import "http://example.org/restaurant"

            param minPrice : EInt

            from Food f
            create Food {
                f.name
            }
            """);

        assertThat(result).hasNoIssues();
        assertThat(result.left().getConforms()).isNotNull();
    }

    @Test
    void strictWithoutConforms() {
        var result = parse("""
            export something to "http://example.com"
            strict to "http://example.org/foodref"
            """);

        assertThat(hasSyntaxError(result.right())).isTrue();
    }

    @Test
    void strictWithoutTo() {
        var result = parse("""
            export something to "http://example.com"
            strict conforms "http://example.org/foodref"
            """);

        assertThat(hasSyntaxError(result.right())).isTrue();
    }

    @Test
    void conformsWithoutUri() {
        var result = parse("""
            export something to "http://example.com"
            conforms to
            """);

        assertThat(hasSyntaxError(result.right())).isTrue();
    }

    @Test
    void strictAfterConforms() {
        var result = parse("""
            export something to "http://example.com"
            conforms strict to "http://example.org/foodref"
            """);

        assertThat(hasSyntaxError(result.right())).isTrue();
    }

    @Test
    void unknownReference() {
        var result = parse("""
            export something to "http://example.com"
            conforms to "http://example.org/unknown"
            """);

        assertThat(result).hasIssues("Reference metamodel not found: http://example.org/unknown");
    }

    @Test
    void unknownReferenceStrict() {
        var result = parse("""
            export something to "http://example.com"
            strict conforms to "http://example.org/unknown"
            """);

        assertThat(result).hasIssues("Reference metamodel not found: http://example.org/unknown");
    }

}
