package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;
import tools.vitruv.neojoin.Constants;

import static org.assertj.core.api.Assertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

/**
 * A reference to an abstract class stays typed with that abstract class. Referencing an abstract class does not
 * create target classes for its sub classes.
 */
class ImplicitAbstractReferenceAQRTest extends AbstractKennelTest {

    /**
     * The implicit target class for Animal is abstract and no sub classes are created.
     */
    @Test
    void referenceToAbstractSourceClassIsTypedWithAbstractTargetClass() {
        var aqr = parse("""
            from Kennel k create KennelT {
                k.name
                k.animals
            }
            """);

        assertThat(aqr)
            .hasRootTargetClass(
                Constants.DefaultRootClassName, root -> {
                    assertThat(root)
                        .hasGeneratedReference("kennelTs", "KennelT")
                        .hasNoMoreFeatures(); // no reference for the abstract class
                }
            )
            .hasTargetClass(
                "KennelT", kennel -> {
                    assertThat(kennel)
                        .hasCopiedAttribute("name", lookup("kennel", "Kennel", "name"))
                        .hasCopiedReference("animals", "Animal", lookup("kennel", "Kennel", "animals"))
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "Animal", animal -> {
                    assertThat(animal)
                        .isAbstract()
                        .sourceIs(lookup("kennel", "Animal"))
                        .sourceExcludesSubClasses()
                        .hasNoSuperClasses();
                }
            )
            .hasNoMoreTargetClasses(); // no Dog, Cat or Puppy
    }

    /**
     * A reference to the source class Animal resolves to the explicit abstract query Animal of the same name, because
     * the abstract query has no source and no query selects the source class Animal. No second, implicit target class
     * named Animal is created.
     */
    @Test
    void referenceResolvesToExplicitAbstractClassOfSameName() {
        var aqr = parse("""
            abstract Animal {
                name: EString
            }

            from Dog d create D extends Animal {
                name = d.name
            }

            from Kennel k create K {
                k.name
                k.animals
            }
            """);

        assertThat(aqr)
            .hasTargetClass("Animal", animal -> assertThat(animal).isAbstract().hasNoSource()) // exactly one
            .hasTargetClass("D", d -> assertThat(d).hasSuperClasses("Animal"))
            .hasTargetClass(
                "K", k -> {
                    assertThat(k)
                        .hasCopiedAttribute("name", lookup("kennel", "Kennel", "name"))
                        .hasCopiedReference("animals", "Animal", lookup("kennel", "Kennel", "animals"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
        var animals = find(aqr, "K").features().stream().filter(f -> f.name().equals("animals")).findFirst().orElseThrow();
        assertThat(((AQRFeature.Reference) animals).type().source()).isNull();
    }

    /**
     * The explicit abstract class Animal counts as the target class of the source class Animal. The implicit class
     * Dog extends it, because it has an equivalent feature for each feature of it.
     */
    @Test
    void implicitClassIsLinkedToExplicitAbstractClassOfSameName() {
        var aqr = parse("""
            abstract Animal {
                name: EString [1]
            }

            from Cat c create C extends Animal {
                name = c.name
            }

            from Kennel k create K {
                k.animals
                k.dogs
            }
            """);

        assertThat(aqr)
            .hasRootTargetClass(
                Constants.DefaultRootClassName, root -> assertThat(root)
                    .hasGeneratedReference("cs", "C")
                    .hasGeneratedReference("ks", "K")
                    .hasGeneratedReference("dogs", "Dog")
                    .hasNoMoreFeatures()
            )
            .hasTargetClass("Animal", animal -> assertThat(animal).isAbstract().hasNoSource())
            .hasTargetClass("C", c -> assertThat(c).hasSuperClasses("Animal"))
            .hasTargetClass("K", k -> assertThat(k).hasCopiedReference("animals", "Animal", lookup("kennel", "Kennel", "animals")))
            .hasTargetClass("Dog", dog -> assertThat(dog).hasSuperClasses("Animal"))
            .hasNoMoreTargetClasses();
    }

}
