package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;

import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

public class ExplicitTypeAQRTest extends AbstractKennelTest {

    /**
     * An explicit class type is accepted if no query selects exactly the source class of the expression, but queries
     * select its sub classes ({@code Dog} for {@code Animal}) or its super class with sub classes ({@code from extends
     * Animal} for {@code Dog}).
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
            .hasTargetClass("K", k -> assertThat(k).hasCalculatedReference("animals", "Animal"))
            .hasTargetClass("L", l -> assertThat(l).hasCopiedReference("star", "Pet", lookup("kennel", "Kennel", "star")));
    }

}
