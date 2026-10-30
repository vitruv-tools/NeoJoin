package tools.vitruv.neojoin.transformation;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.transformation.predicates.InstancePredicates;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tools.vitruv.neojoin.transformation.InstanceModelAssertions.assertThat;

/**
 * Source meta-models may refer to classes of the Ecore meta-model although Ecore is not on the meta-model path.
 * Such references point to the target instance of the referenced source instance.
 */
public class EcoreClassReferenceTest extends AbstractTransformationTest implements InstancePredicates {

    @Override
    protected List<String> getMetaModelPaths() {
        return List.of("/models/eobject-ref.ecore");
    }

    @Override
    protected List<String> getInstanceModelPaths() {
        return List.of("/models/eobject-ref.xmi");
    }

    private void transformAndAssert(String query, Consumer<EObject> assertion) {
        assertion.accept(internalTransform("""
            export package to "http://example.com"

            import "http://vitruv.tools/eobjectref"

            """ + query));
    }

    @Test
    void referenceToEObjectPointsToTargetInstanceOfSelectedSource() {
        transformAndAssert("""
            from Holder h create Holder { h.name h.target }
            """, result -> assertThat(result)
            .hasInstance(
                "Holder", named("b")
            )
            .hasInstance(
                "Holder", named("a"), holder -> assertThat(holder)
                    .hasReference("target", named("b"))
            )
            .hasNoMoreInstances());
    }

    @Test
    void containmentReferenceToEObjectContainsTargetInstanceOfSelectedSource() {
        transformAndAssert("""
            from Container c create Container { c.content }
            from Leaf l create Leaf { l.name }
            """, result -> assertThat(result)
            .hasInstance(
                "Container", c -> true, container -> assertThat(container)
                    .hasReference("content", content -> content != null && content.eContainer() == container)
            )
            .hasInstance("Leaf", named("leaf"))
            .hasNoMoreInstances());
    }

    @Test
    void referenceToEObjectFailsIfReferencedSourceIsNotSelected() {
        assertThatThrownBy(() -> transformAndAssert("""
            from Holder h where h.name == "a" create Holder { h.name h.target }
            """, result -> {})
        ).hasMessage(
            "Failed to transform models: no target instance found for source instance of class 'Holder'"
        );
    }

}
