package tools.vitruv.neojoin.transformation.emf;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static tools.vitruv.neojoin.transformation.emf.InstanceModelAssertions.assertThat;

/**
 * Instance-level behavior of abstract classes and inherited or overridden features.
 */
@SuppressWarnings("unchecked")
class AbstractClassTest extends AbstractKennelTest {

    /**
     * Abstract classes produce no instances. Overriding features (copied, calculated, many-valued and own ones) of two
     * sub classes with different expressions end up in the instances, with multiple inheritance and a transitive chain
     * of abstract classes.
     */
    @Test
    void overridingFeaturesAreEvaluated() {
        var result = transform("""
            abstract Named {
                name: EString
            }

            abstract Described extends Named {
                name: EString
                info: EString
                tags: EString [*]
            }

            abstract Counted {
                size: EInt
            }

            from extends Dog d
            create DogT extends Described, Counted {
                name = d.name
                info := "dog:" + d.breed
                tags := #[d.name, "dog"]
                size := d.name.length
                extra := "x"
            }

            from Cat c
            create CatT extends Described, Counted {
                name = c.name
                info := "cat:" + c.lives
                tags := #["cat"]
                size := 0
            }
            """);

        assertThat(result)
            .hasInstance(
                "DogT", named("Rex"), dog -> {
                    assertThat(dog)
                        .hasAttribute("info", "dog:Lab")
                        .hasAttribute("tags", List.of("Rex", "dog"))
                        .hasAttribute("size", 3)
                        .hasAttribute("extra", "x");
                }
            )
            .hasInstance(
                "DogT", named("Bello"), dog -> {
                    assertThat(dog)
                        .hasAttribute("info", "dog:Pug")
                        .hasAttribute("tags", List.of("Bello", "dog"))
                        .hasAttribute("size", 5)
                        .hasAttribute("extra", "x");
                }
            )
            .hasInstance(
                "CatT", named("Tom"), cat -> {
                    assertThat(cat)
                        .hasAttribute("info", "cat:9")
                        .hasAttribute("tags", List.of("cat"))
                        .hasAttribute("size", 0);
                }
            )
            .hasNoMoreInstances();
    }

    @Test
    void implicitCopyOverridesAbstractFeature() {
        var result = transform("""
            abstract Bred {
                breed: EString
            }

            from Dog
            create extends Bred
            """);

        assertThat(result)
            .hasInstance("Dog", attribute("breed", "Lab"))
            .hasNoMoreInstances();
    }

    @Test
    void overridingFeatureOfConcreteSuperClassUsesOwnExpression() {
        var result = transform("""
            from Dog d
            create DogT {
                name = d.name
                breed = d.breed
            }

            from Puppy p
            create PuppyT extends DogT {
                name = p.name
                breed := p.breed.toUpperCase
                age = p.age
            }
            """);

        assertThat(result)
            .hasInstance("DogT", and(named("Rex"), attribute("breed", "Lab")))
            .hasInstance("PuppyT", and(named("Bello"), attribute("breed", "PUG"), attribute("age", 1)))
            .hasNoMoreInstances();
    }

    /**
     * The modifiers of the overridden feature (many-valued containment) are used for the overriding feature. The
     * instances are contained in the kennel and not in the root.
     */
    @Test
    void inheritedContainmentModifierIsHonored() {
        var result = transform("""
            abstract Holder {
                items: Dog [containment, *]
            }

            from extends Dog d
            create Dog {
                d.name
            }

            from Kennel k
            create KennelT extends Holder {
                k.name
                items = k.dogs
            }
            """);

        Predicate<EObject> inNorth = dog -> dog.eContainer() != null && named("North").test(dog.eContainer());
        assertThat(result)
            .hasInstance(
                "KennelT", named("North"), kennel -> {
                    assertThat(kennel)
                        .hasManyReferenceInstancesOf("items", "Dog", "Dog")
                        .hasManyReference("items", named("Rex"), named("Bello"));
                }
            )
            .hasInstance("KennelT", named("South"))
            .hasInstance("Dog", and(named("Rex"), inNorth))
            .hasInstance("Dog", and(named("Bello"), inNorth))
            .hasNoMoreInstances();
    }

    /**
     * The root class can extend an abstract class and still contains the other classes.
     */
    @Test
    void rootClassWithSuperClassEvaluatesOverridingFeatureAndReferencesOtherClasses() {
        var result = transform("""
            abstract Named {
                name: EString
            }

            create root Zoo extends Named {
                name := "zoo"
            }

            from Dog d
            create DogT {
                d.name
            }
            """);

        assertThat(result)
            .hasAttribute("name", "zoo")
            .hasManyReference("dogTs", named("Rex"))
            .hasInstance("DogT", named("Rex"))
            .hasNoMoreInstances();
    }

}
