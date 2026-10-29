package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * An implicit class or the class of a sub query is linked to the target class of its nearest source super class that has one. If there are
 * several such target classes, the choice is ambiguous. The validator reports the user-facing error.
 */
class ImplicitSuperClassAmbiguityAQRTest extends AbstractKennelAQRTest {

    @Test
    void severalTargetClassesOfNearestSourceSuperClassAreAmbiguous() {
        assertThatThrownBy(() -> buildAQR("""
            from Animal a create A1 {
                a.name
            }

            from Animal a create A2 {
                a.name
            }

            from Kennel k create K {
                k.name
                k.star create {
                    name
                    breed
                }
            }
            """))
            .isInstanceOf(AQRInvariantViolatedException.class)
            .hasMessageContaining("Ambiguous super class for target class 'Dog'")
            .hasMessageContaining("A1, A2");
    }

}
