package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * If several super classes provide a feature with the same name, the AQR accepts it only if the features are
 * equivalent (same kind of feature, same type, equal modifiers). The validator reports the user-facing error.
 */
class DiamondInheritanceAQRTest extends AbstractKennelAQRTest {

    private static AQRTargetClass find(AQR aqr, String name) {
        return aqr.classes().stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

    /**
     * The feature of the first super class (B) is the overridden feature.
     */
    @Test
    void equivalentFeaturesOfSeveralSuperClassesAreAccepted() {
        var aqr = buildAQR("""
            abstract B {
                name: EString [1]
            }

            abstract C {
                name: EString [1]
            }

            from Dog d create D extends B, C {
                name = d.name
            }
            """);

        var name = find(aqr, "D").features().get(0);
        assertThat(name.kind()).isInstanceOf(AQRFeature.Kind.Override.class);
        assertThat(((AQRFeature.Kind.Override) name.kind()).overridden()).isEqualTo(find(aqr, "B").features().get(0));
    }

    @Test
    void featuresWithDifferentModifiersConflict() {
        assertThatThrownBy(() -> buildAQR("""
            abstract B {
                name: EString [1]
            }

            abstract C {
                name: EString [?]
            }

            from Dog d create D extends B, C {
                name = d.name
            }
            """))
            .isInstanceOf(AQRInvariantViolatedException.class)
            .hasMessageContaining("conflicting features named 'name'");
    }

}
