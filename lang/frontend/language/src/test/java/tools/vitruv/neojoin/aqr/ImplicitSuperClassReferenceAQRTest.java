package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractIntegrationTest;

import java.util.List;

import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

/**
 * An implicit class is linked to its candidate super class only if the references of the candidate have an
 * equivalent in the implicit class, i.e. they reference the same target class.
 */
class ImplicitSuperClassReferenceAQRTest extends AbstractIntegrationTest {

    @Override
    protected List<String> getMetaModelPaths() {
        return List.of("/models/eobject-ref.ecore");
    }

    private AQR parse(String query) {
        return internalParseAQR("""
            export package to "http://example.com"

            import "http://vitruv.tools/eobjectref"

            """ + query);
    }

    /**
     * The implicit class SubParent copies the reference child of its source class, as the explicit class Parent does.
     * Both reference the same implicit class Leaf.
     */
    @Test
    void implicitClassIsLinkedToSuperClassWithEquivalentReference() {
        var aqr = parse("""
            from Parent create Parent

            from Box b create B {
                b.parent
            }
            """);

        assertThat(aqr).hasTargetClass("SubParent", subParent -> assertThat(subParent).hasSuperClasses("Parent"));
    }

    /**
     * The implicit class SubHolder copies the references to Ecore classes of its source class, as the explicit class
     * Holder does.
     */
    @Test
    void implicitClassIsLinkedToSuperClassWithEquivalentReferenceToEcoreClass() {
        var aqr = parse("""
            from Holder create Holder

            from Box b create B {
                b.holder
            }
            """);

        assertThat(aqr).hasTargetClass("SubHolder", subHolder -> assertThat(subHolder).hasSuperClasses("Holder"));
    }

}
