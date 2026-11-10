package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.model.AQRFeature;

import static org.assertj.core.api.Assertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

/**
 * If several super classes provide a feature with the same name, they inherit it from the same declaration (common
 * ancestor) and the feature of the first super class is the overridden feature.
 */
class DiamondInheritanceAQRTest extends AbstractKennelTest {

    /**
     * The feature of the first super class (B) is the overridden feature.
     */
    @Test
    void featureOfFirstSuperClassIsOverriddenInDiamond() {
        var aqr = parse("""
            abstract A {
                name: EString [1]
            }

            abstract B extends A {
                name: EString
            }

            abstract C extends A {
                name: EString
            }

            from Dog d create D extends B, C {
                name = d.name
            }
            """);

        assertThat(aqr)
            .hasRootTargetClass(Constants.DefaultRootClassName, root -> assertThat(root).hasGeneratedReference("ds", "D").hasNoMoreFeatures())
            .hasTargetClass("A", a -> assertThat(a).isAbstract().hasAbstractAttribute("name", "EString").hasNoMoreFeatures())
            .hasTargetClass("B", b -> assertThat(b).hasSuperClasses("A").hasOverridingAbstractAttribute("name", "EString").hasNoMoreFeatures())
            .hasTargetClass("C", c -> assertThat(c).hasSuperClasses("A").hasOverridingAbstractAttribute("name", "EString").hasNoMoreFeatures())
            .hasTargetClass(
                "D", d -> assertThat(d)
                    .hasSuperClasses("B", "C")
                    .hasOverridingCopiedAttribute("name", lookup("kennel", "Animal", "name"))
                    .hasNoMoreFeatures()
            )
            .hasNoMoreTargetClasses();

        var name = find(aqr, "D").features().get(0);
        assertThat(((AQRFeature.Kind.Override) name.kind()).overridden()).isEqualTo(find(aqr, "B").features().get(0));
    }

    /**
     * References are overridden like attributes.
     */
    @Test
    void referenceOfFirstSuperClassIsOverriddenInDiamond() {
        var aqr = parse("""
            from Dog create DogT

            abstract A {
                pal: DogT
            }

            abstract B extends A {
                pal: DogT
            }

            abstract C extends A {
                pal: DogT
            }

            from Kennel k create E extends B, C {
                pal = k.star
            }
            """);

        var pal = find(aqr, "E").features().get(0);
        assertThat(((AQRFeature.Kind.Override) pal.kind()).overridden()).isEqualTo(find(aqr, "B").features().get(0));
    }

}
