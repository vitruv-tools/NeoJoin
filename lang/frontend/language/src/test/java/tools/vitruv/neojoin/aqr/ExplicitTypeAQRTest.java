package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;
import tools.vitruv.neojoin.Constants;

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
            .hasTargetClass("Animal", animal -> assertThat(animal).isAbstract().hasAbstractAttribute("name", "EString").hasNoMoreFeatures())
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
            .hasTargetClass("Pet", pet -> assertThat(pet).hasCopiedAttribute("name", lookup("kennel", "Animal", "name")).hasNoMoreFeatures())
            .hasTargetClass(
                "L", l -> assertThat(l)
                    .hasCopiedAttribute("name", lookup("kennel", "Kennel", "name"))
                    .hasCopiedReference("star", "Pet", lookup("kennel", "Kennel", "star"))
                    .hasNoMoreFeatures()
            )
            .hasNoMoreTargetClasses();
    }

}
