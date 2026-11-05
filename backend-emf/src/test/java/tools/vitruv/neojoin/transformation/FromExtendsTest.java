package tools.vitruv.neojoin.transformation;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.transformation.InstanceModelAssertions.assertThat;

/**
 * {@code from X} (direct instances only) in comparison to {@code from extends X} (instances of sub classes too).
 */
@SuppressWarnings("unchecked")
class FromExtendsTest extends AbstractKennelTest {

    @Test
    void fromSelectsDirectInstancesAndFromExtendsAlsoInstancesOfSubClasses() {
        var result = transform("""
            from Dog d
            create Direct {
                d.name
            }

            from extends Dog d
            create AllDogs {
                d.name
            }

            from Animal a
            create DirectAnimals {
                a.name
            }

            from extends Animal a
            create AllAnimals {
                a.name
            }
            """);

        // no direct instances of the abstract class Animal
        assertThat(result)
            .hasInstance("Direct", named("Rex"))
            .hasInstance("AllDogs", named("Rex"))
            .hasInstance("AllDogs", named("Bello"))
            .hasInstance("AllAnimals", named("Rex"))
            .hasInstance("AllAnimals", named("Bello"))
            .hasInstance("AllAnimals", named("Tom"))
            .hasNoMoreInstances();
    }

    @Test
    void joinExtendsSelectsInstancesOfSubClasses() {
        var result = transform("""
            from Kennel k
            join extends Animal a
                on k.animals.contains(a)
            create Housing {
                kennel := k.name
                animal := a.name
            }
            """);

        assertThat(result)
            .hasInstance("Housing", and(attribute("kennel", "North"), attribute("animal", "Rex")))
            .hasInstance("Housing", and(attribute("kennel", "North"), attribute("animal", "Bello")))
            .hasNoMoreInstances();
    }

    /**
     * The implicit copy of sub class instances yields Dog instances for Rex and Bello with their inherited name and
     * their breed.
     */
    @Test
    void implicitCopyOfSubClassInstancesIncludesInheritedSourceFeatures() {
        var result = transform("""
            from extends Dog
            create
            """);

        assertThat(result)
            .hasInstance("Dog", and(named("Rex"), attribute("breed", "Lab")))
            .hasInstance("Dog", and(named("Bello"), attribute("breed", "Pug")))
            .hasNoMoreInstances();
    }

}
