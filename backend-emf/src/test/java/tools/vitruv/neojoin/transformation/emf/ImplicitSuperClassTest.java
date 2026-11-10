package tools.vitruv.neojoin.transformation.emf;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tools.vitruv.neojoin.transformation.emf.InstanceModelAssertions.assertThat;

/**
 * Tests for the super class links of implicit target classes. An implicit target class is linked to the target classes
 * of its source super classes (or the nearest source ancestors that are part of the view type). Selecting a class
 * without its super classes must stay possible and references to untransformed instances are an error.
 */
class ImplicitSuperClassTest extends AbstractKennelTest {

    /**
     * Implicit targets: Animal (abstract), Dog (extends Animal) and Puppy (extends Dog). Each instance is selected by
     * exactly one of them, because implicit classes select only direct instances, so all references can be resolved.
     */
    @Test
    void instancesOfImplicitClassesOfSourceHierarchyAreTransformedOnce() {
        var result = transform("""
            from Kennel create
            """);

        assertThat(result)
            .hasInstance("Kennel", named("North"))
            .hasInstance("Kennel", named("South"))
            .hasInstance("Dog", named("Rex"))
            .hasInstance("Puppy", named("Bello"))
            .hasNoMoreInstances();
    }

    /**
     * The references {@code animals}, {@code dogs}, {@code star} and {@code puppy} of the kennel are copied, so implicit
     * target classes exist for Animal (abstract), Dog (extends Animal) and Puppy (extends Dog). The reference
     * {@code animals} is typed Animal and resolves to the Dog instance Rex and the Puppy instance Bello, which
     * requires the super class links.
     */
    @Test
    void referenceTypedWithAbstractClassResolvesToInstancesOfImplicitSubClasses() {
        var result = transform("""
            from Kennel k
            create KennelT {
                k.name
                k.animals
                k.dogs
                k.star
                k.puppy
            }
            """);

        assertThat(result)
            .hasInstance(
                "KennelT", named("North"), kennel -> {
                    assertThat(kennel)
                        .hasManyReference("animals", named("Rex"), named("Bello"))
                        .hasManyReference("dogs", named("Rex"), named("Bello"))
                        .hasReference("star", named("Rex"))
                        .hasReference("puppy", named("Bello"));
                }
            )
            .hasInstance("KennelT", named("South"))
            .hasInstance("Dog", named("Rex"), dog -> assertThat(dog).hasAttribute("breed", "Lab"))
            .hasInstance("Puppy", named("Bello"), puppy -> assertThat(puppy).hasAttribute("breed", "Pug").hasAttribute("age", 1))
            .hasNoMoreInstances();
    }

    /**
     * Implicit classes select only direct instances: no implicit class exists for Puppy, so the reference
     * {@code dogs} cannot be resolved for Bello.
     */
    @Test
    void referenceToInstanceOfSubClassWithoutTargetFails() {
        assertThatThrownBy(() -> {
            transform("""
                from Kennel k
                create KennelT {
                    k.name
                    k.dogs
                }
                """);
        }).hasMessage(
            "Failed to transform models: no target instance of class 'Dog' found for source instance of class 'Puppy'"
        );
    }

    /**
     * The explicit abstract class Animal is the target class of the source class Animal, so the implicit classes Dog
     * and Puppy extend it and the reference {@code animals} resolves to their instances.
     */
    @Test
    void implicitClassesExtendExplicitAbstractClassOfSameName() {
        var result = transform("""
            abstract Animal {
                name: EString [1]
            }

            from Cat c
            create C extends Animal {
                name = c.name
            }

            from Kennel k
            create KennelT {
                k.name
                k.animals
                k.dogs
                k.puppy
            }
            """);

        assertThat(result)
            .hasInstance(
                "KennelT", named("North"), kennel -> {
                    assertThat(kennel).hasManyReference("animals", named("Rex"), named("Bello"));
                }
            )
            .hasInstance("KennelT", named("South"))
            .hasInstance("C", named("Tom"))
            .hasInstance("Dog", named("Rex"))
            .hasInstance("Puppy", named("Bello"))
            .hasNoMoreInstances();
    }

    /**
     * The explicit class Pet selects all animals ({@code from extends}), so the reference {@code animals} (source type
     * Animal) points to Pet instances.
     */
    @Test
    void referenceToClassSelectedWithFromExtendsResolvesToItsInstances() {
        var result = transform("""
            from extends Animal
            create Pet {
                name
            }

            from Kennel k
            create KennelT {
                k.name
                k.animals
            }
            """);

        assertThat(result)
            .hasInstance(
                "KennelT", named("North"), kennel -> {
                    assertThat(kennel)
                        .hasManyReferenceInstancesOf("animals", "Pet", "Pet")
                        .hasManyReference("animals", named("Rex"), named("Bello"));
                }
            )
            .hasInstance("KennelT", named("South"))
            .hasInstance("Pet", named("Rex"))
            .hasInstance("Pet", named("Bello"))
            .hasInstance("Pet", named("Tom"))
            .hasNoMoreInstances();
    }

    /**
     * Referencing an abstract class does not transform instances: no class selects the dogs that {@code animals}
     * points to.
     */
    @Test
    void referenceToInstanceWithoutTargetFails() {
        // Animal is only implicit (and abstract) and no query transforms the dogs
        assertThatThrownBy(() -> {
            transform("""
                from Kennel k
                create KennelT {
                    k.name
                    k.animals
                }
                """);
        }).hasMessage(
            "Failed to transform models: no target instance of class 'Animal' found for source instance of class 'Dog'"
        );
    }

}
