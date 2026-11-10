package tools.vitruv.neojoin.transformation.emf;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tools.vitruv.neojoin.transformation.emf.InstanceModelAssertions.assertThat;

/**
 * References typed with a super or abstract class that point to instances of sub classes.
 */
class ReferenceInheritanceTest extends AbstractKennelTest {

    /**
     * Multi-valued and single-valued references typed with a concrete super class resolve to instances of its sub
     * classes. Rex is also mapped to the unrelated class DogView, which must not make the reference ambiguous.
     */
    @Test
    void referencesTypedWithSuperClassResolveToInstancesOfSubClasses() {
        var result = transform("""
            from Animal a
            create Pet {
                name = a.name
            }

            from extends Dog d
            create DogPet extends Pet {
                name = d.name
                breed = d.breed
            }

            from Cat c
            create CatPet extends Pet {
                name = c.name
            }

            from extends Dog d
            create DogView {
                d.breed
            }

            from Kennel k
            create KennelT {
                k.name
                animals: Pet [*] = k.animals
                star: Pet = k.star
            }
            """);

        assertThat(result)
            .hasInstance(
                "KennelT", named("North"), kennel -> {
                    assertThat(kennel)
                        .hasManyReferenceInstancesOf("animals", "DogPet", "DogPet")
                        .hasManyReference("animals", named("Rex"), named("Bello"))
                        .hasReferenceInstanceOf("star", "DogPet")
                        .hasReference("star", named("Rex"));
                }
            )
            .hasInstance(
                "KennelT", named("South"), kennel -> {
                    assertThat(kennel)
                        .hasManyReferenceInstancesOf("animals")
                        .hasReference("star", other -> other == null);
                }
            )
            .hasInstance("DogPet", named("Rex"))
            .hasInstance("DogPet", named("Bello"))
            .hasInstance("CatPet", named("Tom"))
            .hasInstance("DogView", attribute("breed", "Lab"))
            .hasInstance("DogView", attribute("breed", "Pug"))
            .hasNoMoreInstances();
    }

    /**
     * Rex is mapped to an instance of AllDogs and one of DirectDogs, which are both instances of the reference type Pet.
     */
    @Test
    void multipleTargetsAssignableToReferenceTypeAreAmbiguous() {
        assertThatThrownBy(() -> {
            transform("""
                from Animal a
                create Pet {
                    name = a.name
                }

                from extends Dog d
                create AllDogs extends Pet {
                    name = d.name
                }

                from Dog d
                create DirectDogs extends Pet {
                    name = d.name
                }

                from Kennel k
                create KennelT {
                    k.name
                    animals: Pet [*] = k.animals
                }
                """);
        }).hasMessage(
            "Failed to transform models: multiple target instances of class 'Pet' found for source instance of class 'Dog'"
        );
    }

    /**
     * A subquery includes instances of sub classes of its source class.
     */
    @Test
    void subQueryIncludesSubClassInstances() {
        var result = transform("""
            from Kennel k
            create KennelT {
                k.name
                k.animals create {
                    it.name
                }
            }
            """);

        assertThat(result)
            .hasInstance(
                "KennelT", named("North"), kennel -> {
                    assertThat(kennel)
                        .hasManyReferenceInstancesOf("animals", "Animal", "Animal")
                        .hasManyReference("animals", named("Rex"), named("Bello"));
                }
            )
            .hasInstance("KennelT", named("South"))
            .hasInstance("Animal", named("Rex"))
            .hasInstance("Animal", named("Bello"))
            .hasInstance("Animal", named("Tom"))
            .hasNoMoreInstances();
    }

}
