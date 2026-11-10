package tools.vitruv.neojoin.aqr;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractIntegrationTest;
import tools.vitruv.neojoin.PackageRegistryUtils;
import tools.vitruv.neojoin.generation.MetaModelGenerator;
import tools.vitruv.neojoin.model.AQR;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

class EcoreClassReferenceAQRTest extends AbstractIntegrationTest implements PackageRegistryUtils {

    @Override
    protected List<String> getMetaModelPaths() {
        // Ecore is deliberately not part of the meta-model paths
        return List.of("/models/eobject-ref.ecore");
    }

    private AQR parse(String query) {
        return internalParseAQR("""
            export package to "http://example.com"

            import "http://vitruv.tools/eobjectref"

            """ + query);
    }

    @Test
    void referenceToEcoreClassIsKeptWithoutTargetClass() {
        var aqr = parse("""
            from Holder h create { h.target }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Holder", holder -> assertThat(holder)
                    .sourceIs(lookup("eobjectref", "Holder"))
                    .hasCopiedEcoreReference(
                        "target",
                        EcorePackage.Literals.EOBJECT,
                        lookup("eobjectref", "Holder", "target")
                    )
                    .hasNoMoreFeatures()
            )
            .hasNoMoreTargetClasses();

        var generated = new MetaModelGenerator(aqr).generate().pack();
        var holder = (EClass) generated.getEClassifier("Holder");
        assertThat(generated.getEClassifier("EObject")).isNull();
        assertThat(holder.getEStructuralFeature("target").getEType()).isSameAs(EcorePackage.Literals.EOBJECT);
    }

    @Test
    void implicitCopyKeepsReferenceToEcoreClass() {
        var aqr = parse("""
            from Holder create Holder
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Holder", holder -> assertThat(holder)
                    .sourceIs(lookup("eobjectref", "Holder"))
                    .hasCopiedAttribute("name", lookup("eobjectref", "Holder", "name"))
                    .hasCopiedEcoreReference(
                        "target",
                        EcorePackage.Literals.EOBJECT,
                        lookup("eobjectref", "Holder", "target")
                    )
                    .hasCopiedEcoreReference(
                        "metaClass",
                        EcorePackage.Literals.ECLASS,
                        lookup("eobjectref", "Holder", "metaClass")
                    )
                    .hasNoMoreFeatures()
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void referenceToEcoreClassCanBeOverridden() {
        var aqr = parse("""
            from Holder h create A { h.target }

            from Holder h create B extends A { h.target }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "A", a -> assertThat(a)
                    .hasCopiedEcoreReference("target", EcorePackage.Literals.EOBJECT, lookup("eobjectref", "Holder", "target"))
                    .hasNoMoreFeatures()
            )
            .hasTargetClass(
                "B", b -> assertThat(b)
                    .hasSuperClasses("A")
                    .hasOverridingCopiedEcoreReference("target", EcorePackage.Literals.EOBJECT, lookup("eobjectref", "Holder", "target"))
                    .hasNoMoreFeatures()
            )
            .hasNoMoreTargetClasses();
    }

}
