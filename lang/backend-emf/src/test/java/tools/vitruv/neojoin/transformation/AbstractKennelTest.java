package tools.vitruv.neojoin.transformation;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.neojoin.transformation.predicates.InstancePredicates;

import java.util.List;

/**
 * Base class for transformation tests using the {@code kennel.ecore} source meta-model.
 * <pre>
 * Animal (abstract) {name}
 *   Dog {breed}
 *     Puppy {age}
 *   Cat {lives}
 * Kennel {name, animals: Animal[*], dogs: Dog[*], star: Dog, puppy: Puppy}
 * Park {kennels: Kennel[*], animals: Animal[*]} (root)
 * </pre>
 * Instances ({@code kennel.xmi}): Dog "Rex" (Lab), Puppy "Bello" (Pug, 1), Cat "Tom" (9 lives), Kennel "North"
 * (animals = Rex, Bello; dogs = Rex, Bello; star = Rex; puppy = Bello) and Kennel "South" (empty).
 */
public abstract class AbstractKennelTest extends AbstractTransformationTest implements InstancePredicates {

    @Override
    protected List<String> getMetaModelPaths() {
        return List.of("/models/kennel.ecore");
    }

    @Override
    protected List<String> getInstanceModelPaths() {
        return List.of("/models/kennel.xmi");
    }

    protected EObject transform(String query) {
        return internalTransform("""
            export package to "http://example.com"

            import "http://example.org/kennel"

            """ + query);
    }

}
