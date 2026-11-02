package tools.vitruv.neojoin.transformation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tools.vitruv.neojoin.transformation.InstanceModelAssertions.assertThat;

/**
 * References typed with a super or abstract class that point to instances of sub classes.
 */
public class ReferenceInheritanceTest extends AbstractKennelTest {

    /**
     * Multi-valued and single-valued references typed with a concrete super class resolve to instances of its sub
     * classes. Rex is also mapped to the unrelated class DogView, which must not make the reference ambiguous.
     */
    @Test
    void referencesTypedWithSuperClass() {
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
                        .hasOnlySubClassInstancesInReference("animals")
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
            .hasInstances("DogView", 2)
            .hasNoMoreInstances();
    }

    /**
     * Same with an explicit abstract class as reference type, which has no instances itself and is not the source
     * class of any query.
     */
    @Test
    void referencesTypedWithAbstractClass() {
        var result = transform("""
            abstract Animal {
                name: EString
            }

            from extends Dog d
            create DogT extends Animal {
                name = d.name
            }

            from Kennel k
            create KennelT {
                k.name
                animals: Animal [*] = k.animals
                star: Animal = k.star
            }
            """);

        assertThat(result)
            .hasInstance(
                "KennelT", named("North"), kennel -> {
                    assertThat(kennel)
                        .hasManyReferenceInstancesOf("animals", "DogT", "DogT")
                        .hasManyReference("animals", named("Rex"), named("Bello"))
                        .hasOnlySubClassInstancesInReference("animals")
                        .hasReference("star", named("Rex"));
                }
            )
            .hasInstance("KennelT", named("South"))
            .hasInstance("DogT", named("Rex"))
            .hasInstance("DogT", named("Bello"))
            .hasNoMoreInstances();
    }

    /**
     * A copied reference without explicit type, whose source class is Animal, resolves to the explicit abstract class
     * of the same name.
     */
    @Test
    void referenceResolvesToExplicitAbstractClassOfSameName() {
        var result = transform("""
            abstract Animal {
                name: EString
            }

            from extends Dog d
            create DogT extends Animal {
                name = d.name
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
                        .hasManyReferenceInstancesOf("animals", "DogT", "DogT")
                        .hasManyReference("animals", named("Rex"), named("Bello"));
                }
            )
            .hasInstance("KennelT", named("South"))
            .hasInstance("DogT", named("Rex"))
            .hasInstance("DogT", named("Bello"))
            .hasNoMoreInstances();
    }

    /**
     * Overriding a reference of an abstract class with a value of a sub class type. The overriding reference has the
     * type of the overridden reference.
     */
    @Test
    void overridingReferenceTakesTypeOfOverriddenReference() {
        var result = transform("""
            abstract Holder {
                animals: Pet [*]
            }

            from Animal a
            create Pet {
                name = a.name
            }

            from extends Dog d
            create DogPet extends Pet {
                name = d.name
            }

            from Kennel k
            create KennelT extends Holder {
                k.name
                animals = k.dogs
            }
            """);

        assertThat(result)
            .hasInstance(
                "KennelT", named("North"), kennel -> {
                    assertThat(kennel)
                        .hasManyReferenceInstancesOf("animals", "DogPet", "DogPet")
                        .hasManyReference("animals", named("Rex"), named("Bello"));
                }
            )
            .hasInstance(
                "KennelT", named("South"), kennel -> {
                    assertThat(kennel).hasManyReferenceInstancesOf("animals");
                }
            )
            .hasInstance("DogPet", named("Rex"))
            .hasInstance("DogPet", named("Bello"))
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
