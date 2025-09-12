package tools.vitruv.neojoin.transformation;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.neojoin.transformation.predicates.InstancePredicates;

import java.util.List;

public abstract class DefaultTransformationTest extends AbstractTransformationTest implements InstancePredicates {

    @Override
    protected List<String> getMetaModelPaths() {
        return List.of("/models/restaurant.ecore", "/models/reviewpage.ecore", "/models/type-casts.ecore");
    }

    @Override
    protected List<String> getInstanceModelPaths() {
        return List.of("/models/restaurants.xmi", "/models/reviews.xmi", "/models/type-casts.xmi");
    }

    protected EObject transform(String query) {
        return internalTransform("""
            export package to "http://example.com"

            import "http://example.org/restaurant"
            import "http://example.org/reviewpage"

            """ + query);
    }

}
