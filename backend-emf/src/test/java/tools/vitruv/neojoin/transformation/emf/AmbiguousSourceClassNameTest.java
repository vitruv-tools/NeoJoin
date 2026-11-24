package tools.vitruv.neojoin.transformation.emf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Transformation errors write the source class the way a query does. Both meta-models have a class {@code Container}.
 */
class AmbiguousSourceClassNameTest extends AbstractTransformationTest {

    @Override
    protected List<String> getMetaModelPaths() {
        return List.of("/models/eobject-ref.ecore", "/models/type-casts.ecore");
    }

    @Override
    protected List<String> getInstanceModelPaths() {
        return List.of("/instances/eobject-ref-container-target.xmi");
    }

    @Test
    void sourceClassWithAmbiguousNameIsWrittenQualified() {
        assertThatThrownBy(() -> internalTransform("""
            export package to "http://example.com"

            import "http://vitruv.tools/eobjectref"
            import "http://vitruv.tools/typecasts"

            from eobjectref.Holder h create Holder { h.name h.target }
            """)
        ).hasMessage(
            "Failed to transform models: no target instance found for source instance of class "
                + "'eobjectref.Container'"
        );
    }

}
