package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;
import tools.vitruv.neojoin.Constants;

import static org.assertj.core.api.Assertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

/**
 * If several super classes provide a feature with the same name and the features are equivalent (same kind of
 * feature, same type, equal modifiers), the feature of the first super class is the overridden feature.
 */
class DiamondInheritanceAQRTest extends AbstractKennelTest {

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

        assertThat(aqr)
            .hasRootTargetClass(Constants.DefaultRootClassName, root -> assertThat(root).hasGeneratedReference("ds", "D").hasNoMoreFeatures())
            .hasTargetClass("B", b -> assertThat(b).isAbstract().hasAbstractAttribute("name", "EString").hasNoMoreFeatures())
            .hasTargetClass("C", c -> assertThat(c).isAbstract().hasAbstractAttribute("name", "EString").hasNoMoreFeatures())
            .hasTargetClass(
                "D", d -> assertThat(d)
                    .hasSuperClasses("B", "C")
                    .hasOverridingCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                    .hasNoMoreFeatures()
            )
            .hasNoMoreTargetClasses();

        var name = find(aqr, "D").features().get(0);
        assertThat(name.kind()).isInstanceOf(AQRFeature.Kind.Override.class);
        assertThat(((AQRFeature.Kind.Override) name.kind()).overridden()).isEqualTo(find(aqr, "B").features().get(0));
    }

    /**
     * References of several super classes are equivalent if they have the same target class.
     */
    @Test
    void equivalentReferencesOfSeveralSuperClassesAreAccepted() {
        var aqr = parse("""
            from Dog create DogT

            abstract B {
                pal: DogT
            }

            abstract C {
                pal: DogT
            }

            from Kennel k create E extends B, C {
                pal = k.star
            }
            """);

        var pal = find(aqr, "E").features().get(0);
        assertThat(pal.kind()).isInstanceOf(AQRFeature.Kind.Override.class);
        assertThat(((AQRFeature.Kind.Override) pal.kind()).overridden()).isEqualTo(find(aqr, "B").features().get(0));
    }

}
