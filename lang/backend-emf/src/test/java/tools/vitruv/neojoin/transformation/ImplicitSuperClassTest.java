package tools.vitruv.neojoin.transformation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tools.vitruv.neojoin.transformation.InstanceModelAssertions.assertThat;

/**
 * Tests for the super class links of implicit target classes. An implicit target class is linked to the target classes
 * of its source super classes (or the nearest source ancestors that are part of the view type). Selecting a class
 * without its super classes must stay possible and references to untransformed instances are an error.
 */
public class ImplicitSuperClassTest extends AbstractKennelTest {

    /**
     * Implicit targets: Animal (abstract), Dog (extends Animal) and Puppy (extends Dog). Rex is mapped to a Dog, Bello
     * is selected by the implicit Dog target (implicit classes include sub classes) AND by the implicit Puppy target.
     * Resolving the reference {@code animals} (typed Animal) for Bello therefore finds two target instances that are
     * both instances of Animal, which {@code TargetMap} reports as ambiguous ("multiple target instances of class
     * 'Animal' found for source instance of class 'Puppy'").
     */
    @Test
    void referenceToInstanceSelectedByMultipleImplicitTargetsIsAmbiguous() {
        assertThatThrownBy(() -> {
            transform("""
                from Kennel create
                """);
        }).hasMessage(
            "Failed to transform models: multiple target instances of class 'Animal' found for source instance of class 'Puppy'"
        );
    }

    /**
     * Only the references {@code animals}, {@code dogs} and {@code star} of the kennel are copied, so implicit target
     * classes exist for Animal (abstract) and Dog (extends Animal, copies the inherited name as override), but not for
     * Puppy. Rex and Bello are both selected by the Dog target only (implicit classes include sub classes) and are
     * mapped to exactly one Dog target instance each. The reference {@code animals} is typed Animal and has to resolve
     * to these Dog instances, which requires the super class link from Dog to Animal.
     * <p>
     * The result contains the Dog instances Rex (Lab) and Bello (Pug) with name and breed, North references them via
     * animals and dogs, star is Rex, and there is no instance of the abstract Animal.
     */
    @Test
    void referenceTypedWithAbstractClassResolvesToImplicitSubClassTarget() {
        var result = transform("""
            from Kennel k
            create KennelT {
                k.name
                k.animals
                k.dogs
                k.star
            }
            """);

        assertThat(result)
            .hasInstance(
                "KennelT", named("North"), kennel -> {
                    assertThat(kennel)
                        .hasManyReferenceInstancesOf("animals", "Dog", "Dog")
                        .hasManyReference("animals", named("Rex"), named("Bello"))
                        .hasManyReference("dogs", named("Rex"), named("Bello"))
                        .hasReference("star", named("Rex"));
                }
            )
            .hasInstance(
                "KennelT", named("South"), kennel -> {
                    assertThat(kennel).hasManyReferenceInstancesOf("animals").hasManyReferenceInstancesOf("dogs");
                }
            )
            .hasInstance(
                "Dog", named("Rex"), dog -> {
                    assertThat(dog).hasAttribute("breed", "Lab");
                }
            )
            .hasInstance(
                "Dog", named("Bello"), dog -> {
                    assertThat(dog).hasAttribute("breed", "Pug");
                }
            )
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
     * The implicit copy of Dog contains the inherited name: a Dog instance for Rex with name and breed and no Animal
     * class.
     */
    @Test
    void implicitCopyIncludesInheritedSourceFeatures() {
        var result = transform("""
            from Dog create
            """);

        assertThat(result)
            .hasInstance(
                "Dog", any(), dog -> {
                    assertThat(dog).hasAttribute("name", "Rex").hasAttribute("breed", "Lab");
                }
            )
            .hasNoMoreInstances();
    }

    /**
     * Referencing a class does not transform its instances: no query selects the dogs that {@code animals} points to.
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
