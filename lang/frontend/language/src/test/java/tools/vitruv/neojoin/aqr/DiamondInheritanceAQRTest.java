package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * If several super classes provide a feature with the same name and the features are equivalent (same kind of
 * feature, same type, equal modifiers), the feature of the first super class is the overridden feature.
 */
class DiamondInheritanceAQRTest extends AbstractKennelTest {

    private static AQRTargetClass find(AQR aqr, String name) {
        return aqr.classes().stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

    /**
     * The feature of the first super class (B) is the overridden feature.
     */
    @Test
    void equivalentFeaturesOfSeveralSuperClassesAreAccepted() {
        var aqr = parse("""
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

}
