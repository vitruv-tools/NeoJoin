package tools.vitruv.neojoin.parse;

import org.eclipse.xtext.validation.Issue;
import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractIntegrationTest;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.utils.Pair;

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

    private Pair<ViewTypeDefinition, List<Issue>> parse(String query) {
        return internalParse("""
            export package to "http://example.com"

            import "http://vitruv.tools/eobjectref"

            """ + query);
    }

    private void assertNoIssues(String query) {
        assertThat(parse(query)).hasNoIssues();
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

    @Test
    void referenceToEcoreClassCanOverrideReferenceToEcoreClass() {
        assertNoIssues("""
            from Holder h create A { h.target }

            from Holder h create B extends A { h.target }
            """);
    }

    @Test
    void referenceToTargetClassCannotOverrideReferenceToEcoreClass() {
        var result = parse("""
            from Leaf create LeafT

            from Holder h create A { h.target }

            from Holder h create B extends A {
                target: LeafT := null
            }
            """);

        assertThat(result).hasExactlyIssues(
            "Reference 'target' to a target class cannot override a reference to an Ecore class"
        );
    }

    @Test
    void referenceToEcoreClassCannotOverrideReferenceToTargetClass() {
        var result = parse("""
            from Leaf create LeafT

            abstract A { target: LeafT }

            from Holder h create B extends A { h.target }
            """);

        assertThat(result).hasExactlyIssues(
            "Reference 'target' to an Ecore class cannot override a reference to a target class"
        );
    }

}
