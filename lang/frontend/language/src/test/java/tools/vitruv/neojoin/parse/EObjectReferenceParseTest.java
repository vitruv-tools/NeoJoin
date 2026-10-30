package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractIntegrationTest;

import java.util.List;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

/**
 * Source meta-models may refer to classes of the Ecore meta-model although Ecore is not on the meta-model path.
 */
class EObjectReferenceParseTest extends AbstractIntegrationTest {

    @Override
    protected List<String> getMetaModelPaths() {
        // Ecore is deliberately not part of the meta-model paths
        return List.of("/models/eobject-ref.ecore");
    }

    private void assertNoIssues(String query) {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://vitruv.tools/eobjectref"

            """ + query);

        assertThat(result).hasNoIssues();
    }

    @Test
    void referenceToEObject() {
        assertNoIssues("""
            from Holder h create { h.name h.target }
            """);
    }

    @Test
    void referenceToOtherEcoreClass() {
        assertNoIssues("""
            from Holder h create { h.name h.metaClass }
            """);
    }

    @Test
    void classWithEcoreSuperType() {
        assertNoIssues("""
            from Named n create { n.name }
            """);
    }

}
