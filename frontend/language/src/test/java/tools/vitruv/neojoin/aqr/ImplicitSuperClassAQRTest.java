package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.model.AQRFeature;
import tools.vitruv.neojoin.model.AQRTargetClass;

import static org.assertj.core.api.Assertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

/**
 * An implicit target class (but not the class of a subquery) gets super class links to the target classes of its
 * source super classes if those are part of the view type. If the direct source super class is not part of the view,
 * the nearest source ancestor that is part of the view is used. Selecting a class alone does not create target
 * classes for its super classes.
 */
class ImplicitSuperClassAQRTest extends AbstractKennelTest {

    /**
     * Implicit Dog extends Animal and Puppy extends Dog, with the inherited features overridden by implicit copies.
     */
    @Test
    void implicitClassesAreLinkedToImplicitSuperClasses() {
        var aqr = parse("""
            from Kennel create
            """);

        assertThat(aqr)
            .hasRootTargetClass(
                Constants.DefaultRootClassName, root -> {
                    assertThat(root)
                        .hasGeneratedReference("kennels", "Kennel")
                        .hasGeneratedReference("dogs", "Dog")
                        .hasGeneratedReference("puppys", "Puppy")
                        .hasNoMoreFeatures(); // no reference for the abstract class Animal
                }
            )
            .hasTargetClass("Kennel", kennel -> {})
            .hasTargetClass(
                "Animal", animal -> {
                    assertThat(animal)
                        .isAbstract()
                        .hasNoSuperClasses()
                        .hasCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "Dog", dog -> {
                    assertThat(dog)
                        .isNotAbstract()
                        .hasSuperClasses("Animal")
                        .hasOverridingImplicitlyCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                        .hasCopiedAttribute("breed", lookup("kennel", "Dog", "breed"))
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "Puppy", puppy -> {
                    assertThat(puppy)
                        .hasSuperClasses("Dog")
                        .hasAllSuperClasses("Dog", "Animal")
                        .hasOverridingImplicitlyCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                        .hasOverridingImplicitlyCopiedAttribute("breed", lookup("kennel", "Dog", "breed"))
                        .hasCopiedAttribute("age", lookup("kennel", "Puppy", "age"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    /**
     * The implicit Dog extends the explicit target class Pet of its source super class Animal.
     */
    @Test
    void implicitClassIsLinkedToExplicitSuperClass() {
        var aqr = parse("""
            from extends Animal a create Pet {
                a.name
            }

            from Kennel create
            """);

        assertThat(aqr)
            .hasTargetClass("Pet", pet -> assertThat(pet).isNotAbstract().hasNoSuperClasses())
            .hasTargetClass("Kennel", kennel -> {})
            .hasTargetClass(
                "Dog", dog -> {
                    assertThat(dog)
                        .hasSuperClasses("Pet")
                        .hasOverridingImplicitlyCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                        .hasCopiedAttribute("breed", lookup("kennel", "Dog", "breed"))
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "Puppy", puppy -> {
                    assertThat(puppy)
                        .hasSuperClasses("Dog")
                        .hasAllSuperClasses("Dog", "Pet")
                        .hasOverridingImplicitlyCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                        .hasOverridingImplicitlyCopiedAttribute("breed", lookup("kennel", "Dog", "breed"))
                        .hasCopiedAttribute("age", lookup("kennel", "Puppy", "age"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    /**
     * If the direct source super class (Dog) is not part of the view, the nearest source ancestor that is (Animal) is
     * used. Puppy extends Animal and there is no Dog.
     */
    @Test
    void implicitClassIsLinkedToNearestAncestorInView() {
        var aqr = parse("""
            from Kennel k create KennelT {
                k.name
                k.animals
                k.puppy
            }
            """);

        assertThat(aqr.classes()).extracting(AQRTargetClass::name)
            .containsExactlyInAnyOrder("KennelT", "Animal", "Puppy", Constants.DefaultRootClassName);
        assertThat(aqr).hasTargetClass(
            "Puppy", puppy -> assertThat(puppy)
                .hasSuperClasses("Animal")
                .hasOverridingImplicitlyCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                .hasCopiedAttribute("breed", lookup("kennel", "Dog", "breed"))
                .hasCopiedAttribute("age", lookup("kennel", "Puppy", "age"))
                .hasNoMoreFeatures()
        );
    }

    /**
     * Super classes are never created or linked when they are not part of the view: selecting a class alone and
     * referencing a class alone create no target class for Animal and no super class links. Implicit target classes
     * select only direct instances.
     */
    @Test
    void noSuperClassWhenSourceSuperClassIsNotInView() {
        var aqr = parse("""
            from Puppy create

            from Kennel k create KennelT {
                k.name
                k.dogs
            }
            """);

        assertThat(aqr)
            .hasTargetClass("KennelT", kennel -> assertThat(kennel).hasCopiedReference("dogs", "Dog", lookup("kennel", "Kennel", "dogs")))
            .hasTargetClass(
                "Puppy", puppy -> {
                    assertThat(puppy)
                        .sourceIs(lookup("kennel", "Puppy"))
                        .sourceExcludesSubClasses()
                        .hasNoSuperClasses();
                }
            )
            .hasTargetClass(
                "Dog", dog -> {
                    assertThat(dog)
                        .sourceIs(lookup("kennel", "Dog"))
                        .sourceExcludesSubClasses()
                        .hasNoSuperClasses();
                }
            )
            .hasNoMoreTargetClasses(); // no Animal
    }

    /**
     * An explicit query defines its super classes itself: it gets none, even though an implicit target class for its
     * source super class exists.
     */
    @Test
    void explicitQueryWithoutExtendsHasNoSuperClasses() {
        var aqr = parse("""
            from Dog create

            from Kennel k create KennelT {
                k.name
                k.animals
            }
            """);

        assertThat(aqr)
            .hasTargetClass("Animal", animal -> assertThat(animal).isAbstract()) // implicit
            .hasTargetClass(
                "Dog", dog -> {
                    assertThat(dog)
                        .sourceIs(lookup("kennel", "Dog"))
                        .sourceExcludesSubClasses()
                        .hasNoSuperClasses();
                }
            )
            .hasTargetClass("KennelT", kennel -> {})
            .hasNoMoreTargetClasses();
    }

    /**
     * Subqueries cannot declare {@code extends}, so their classes get no super classes, even though the explicit
     * class Pet exists for the source super class Animal. Pet has a feature that the subquery class Dog does not
     * declare, which would violate the rule that sub classes override all inherited features if Dog extended Pet.
     * Subqueries select direct instances and instances of sub classes.
     */
    @Test
    void subQueryClassHasNoSuperClasses() {
        var aqr = parse("""
            from extends Animal a create Pet {
                a.name
                tag = a.name
            }

            from Kennel k create KennelT {
                k.name
                k.star create {
                    name
                    breed
                }
            }
            """);

        var dog = find(aqr, "Dog");
        assertThat(dog).hasNoSuperClasses();
        assertThat(dog.features()).extracting(AQRFeature::name).containsExactlyInAnyOrder("name", "breed");
        assertThat(dog).sourceIncludesSubClasses();
    }

    /**
     * The implicit class Dog is not linked to the nearest candidate, the subquery class Animal, because it cannot
     * override the feature tag of Animal.
     */
    @Test
    void implicitClassIsNotLinkedToSuperClassWithFeaturesItCannotOverride() {
        var aqr = parse("""
            from Kennel k create KennelT {
                k.name
                k.animals create Animal {
                    name = it.name
                    tag = it.name
                }
                k.dogs
            }
            """);

        assertThat(find(aqr, "Dog")).hasNoSuperClasses();
        assertThat(find(aqr, "Animal")).hasNoSuperClasses();
    }

    /**
     * Queries with joins are no super class candidates: they do not select exactly one source class. The implicit
     * class Dog is not linked to the query Mix, whose source class is Animal.
     */
    @Test
    void implicitClassIsNotLinkedToQueryWithJoin() {
        var aqr = parse("""
            from extends Animal a
            join Kennel k on k.name == a.name
            create Mix {
                n = a.name
            }

            from Kennel k create K {
                k.dogs
            }
            """);

        assertThat(find(aqr, "Dog")).hasNoSuperClasses();
    }

    /**
     * The implicit class Puppy is not linked to the explicit class Dog, because name has another type in Dog.
     */
    @Test
    void implicitClassIsNotLinkedToSuperClassWithFeatureOfDifferentType() {
        var aqr = parse("""
            from Dog d create Dog {
                name := d.name.length
            }

            from Kennel k create K {
                k.puppy
            }
            """);

        assertThat(find(aqr, "Puppy")).hasNoSuperClasses();
    }

    /**
     * The implicit class Puppy is not linked to the farther candidate Animal after the nearest candidate Dog was
     * rejected, although Puppy could override all features of Animal.
     */
    @Test
    void implicitClassIsNotLinkedToFartherSuperClassIfNearestCandidateIsRejected() {
        var aqr = parse("""
            from Dog d create Dog {
                name := d.name.length
            }

            from extends Animal a create Animal {
                name = a.name
            }

            from Kennel k create K {
                k.puppy
            }
            """);

        assertThat(find(aqr, "Puppy")).hasNoSuperClasses();
    }

    /**
     * The implicit class Puppy is not linked to the explicit class Dog, because breed has other modifiers in Dog.
     */
    @Test
    void implicitClassIsNotLinkedToSuperClassWithFeatureOfDifferentModifiers() {
        var aqr = parse("""
            from Dog d create Dog {
                name = d.name
                breed: EString [1] := d.breed
            }

            from Kennel k create K {
                k.puppy
            }
            """);

        assertThat(find(aqr, "Puppy")).hasNoSuperClasses();
    }

}
