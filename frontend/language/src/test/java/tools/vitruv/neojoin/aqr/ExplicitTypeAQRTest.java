package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;
import tools.vitruv.neojoin.Constants;

import static org.assertj.core.api.Assertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

class ExplicitTypeAQRTest extends AbstractKennelTest {

    /**
     * An explicit class type is accepted if no query with a target class assignable to the type selects exactly the
     * source class of the expression, but a query selects its sub classes ({@code Dog} for {@code Animal}) or its super
     * class with sub classes ({@code from extends Animal} for {@code Dog}).
     */
    @Test
    void explicitTypeIsAcceptedWhenOnlySubClassOrSuperClassWithSubClassesIsSelected() {
        var aqr = parse("""
            abstract Animal {
                name: EString
            }

            from Dog d create D extends Animal {
                name = d.name
            }

            from Kennel k create K {
                k.name
                animals: Animal := k.animals
            }

            from extends Animal a create Pet {
                a.name
            }

            from Kennel k create L {
                k.name
                star: Pet = k.star
            }
            """);

        assertThat(aqr)
            .hasRootTargetClass(
                Constants.DefaultRootClassName, root -> {
                    assertThat(root)
                        .hasGeneratedReference("ds", "D")
                        .hasGeneratedReference("ks", "K")
                        .hasGeneratedReference("pets", "Pet")
                        .hasGeneratedReference("ls", "L")
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "Animal",
                animal -> assertThat(animal).isAbstract().hasAbstractAttribute("name", "EString").hasNoMoreFeatures()
            )
            .hasTargetClass(
                "D", d -> assertThat(d)
                    .hasSuperClasses("Animal")
                    .hasOverridingCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                    .hasNoMoreFeatures()
            )
            .hasTargetClass(
                "K", k -> assertThat(k)
                    .hasCopiedAttribute("name", lookup("kennel", "Kennel", "name"))
                    .hasCalculatedReference("animals", "Animal")
                    .hasNoMoreFeatures()
            )
            .hasTargetClass(
                "Pet",
                pet -> assertThat(pet)
                    .hasCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                    .hasNoMoreFeatures()
            )
            .hasTargetClass(
                "L", l -> assertThat(l)
                    .hasCopiedAttribute("name", lookup("kennel", "Kennel", "name"))
                    .hasCopiedReference("star", "Pet", lookup("kennel", "Kennel", "star"))
                    .hasNoMoreFeatures()
            )
            .hasNoMoreTargetClasses();
    }

    /**
     * The explicit type {@code Animal} names the implicit class that the copied reference {@code animals} creates for
     * the source class {@code Animal}: both features reference the same target class.
     */
    @Test
    void implicitClassAsExplicitTypeIsTheImplicitTargetClass() {
        var aqr = parse("""
            from Kennel k create K {
                k.animals
                first: Animal := k.animals.head
            }
            """);

        var animal = find(aqr, "Animal");
        var k = find(aqr, "K");

        assertThat(aqr)
            .hasRootTargetClass(
                Constants.DefaultRootClassName, root -> {
                    assertThat(root)
                        .hasGeneratedReference("ks", "K")
                        .hasNoMoreFeatures(); // no reference for the abstract class Animal
                }
            )
            .hasTargetClass(
                "K", targetClass -> assertThat(targetClass)
                    .hasCopiedReference("animals", "Animal", lookup("kennel", "Kennel", "animals"))
                    .hasCalculatedReference("first", "Animal")
                    .hasNoMoreFeatures()
            )
            .hasTargetClass(
                "Animal", a -> assertThat(a)
                    .isAbstract()
                    .hasCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                    .hasNoMoreFeatures()
            )
            .hasNoMoreTargetClasses();

        assertThat(assertThat(k).getReference("first", "Animal").type()).isSameAs(animal);
    }

}
