package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;
import tools.vitruv.neojoin.Constants;

import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

/**
 * A reference to an abstract class stays typed with that abstract class. Referencing an abstract class does not
 * create target classes for its sub classes.
 */
public class ImplicitAbstractReferenceAQRTest extends AbstractKennelTest {

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
                        .sourceIncludesSubClasses()
                        .hasNoSuperClasses();
                }
            )
            .hasNoMoreTargetClasses(); // no Dog, Cat or Puppy
    }

    /**
     * An explicitly declared abstract class is the explicit type of the reference. The reference is typed with the
     * abstract target class and no further classes are created.
     */
    @Test
    void referenceWithExplicitAbstractType() {
        var aqr = parse("""
            abstract Animal {
                name: EString
            }

            from Dog d create D extends Animal {
                name = d.name
            }

            from Kennel k create KennelT {
                k.name
                animals: Animal := k.animals
            }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "KennelT", kennel -> {
                    assertThat(kennel)
                        .hasCopiedAttribute("name", lookup("kennel", "Kennel", "name"))
                        .hasCalculatedReference("animals", "Animal")
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass("Animal", animal -> assertThat(animal).isAbstract().hasNoSource())
            .hasTargetClass("D", d -> assertThat(d).hasSuperClasses("Animal"))
            .hasNoMoreTargetClasses();
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
        var animals = aqr.classes().stream().filter(c -> c.name().equals("K")).findFirst().orElseThrow()
            .features().stream().filter(f -> f.name().equals("animals")).findFirst().orElseThrow();
        org.assertj.core.api.Assertions.assertThat(((AQRFeature.Reference) animals).type().source()).isNull();
    }

}
