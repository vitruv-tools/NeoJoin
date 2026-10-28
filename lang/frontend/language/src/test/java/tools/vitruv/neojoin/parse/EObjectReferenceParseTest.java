package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractIntegrationTest;

import java.util.List;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

/**
 * References typed with Ecore's own {@code EObject} (as in Vitruvius' correspondence meta-model) are typed with the
 * {@code EObject} Java interface, although Ecore itself is not a source meta-model.
 */
class EObjectReferenceParseTest extends AbstractIntegrationTest {

    @Override
    protected List<String> getMetaModelPaths() {
        return List.of(
            "/models/restaurant.ecore",
            "/models/eobject-ref.ecore"
        );
    }

    @Test
    void joinOnEObjectReferences() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://example.org/restaurant"
            import "http://example.org/eobjectref"

            from Restaurant r
            join Link l on l.source == r || l.targets.contains(r)
            create {
                r.name
                tag := l.tag
            }
            """);

        assertThat(result).hasNoIssues();
    }

}
