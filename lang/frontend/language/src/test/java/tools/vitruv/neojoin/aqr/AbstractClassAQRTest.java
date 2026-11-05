package tools.vitruv.neojoin.aqr;

import org.eclipse.emf.ecore.ETypedElement;
import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.AbstractKennelTest;
import tools.vitruv.neojoin.Constants;
import tools.vitruv.neojoin.aqr.AQRFeatureOptionsAssertions.Mod;

import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRFeatureOptionsAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

class AbstractClassAQRTest extends AbstractKennelTest {

    /**
     * Abstract classes (no source, abstract features with all kinds of modifiers, references to concrete and abstract
     * classes) are not referenced from the root, whereas a concrete query from an abstract source class is.
     */
    @Test
    void abstractClassHasAbstractFeaturesAndNoRootReference() {
        var aqr = parse("""
            from Dog create Doggo

            from Animal create Pet

            abstract Shelter {
                label: EString [1]
                b: EInt [*, !ordered]
                c: EString [volatile, transient, unsettable, derived, !changeable, id]
                d: EInt [2..5, !unique]
                mascot: Doggo [containment, 1]
                residents: Doggo [+, !ordered]
                parent: Shelter
            }
            """);

        assertThat(aqr)
            .hasRootTargetClass(
                Constants.DefaultRootClassName, root -> {
                    assertThat(root)
                        .hasGeneratedReference("doggos", "Doggo")
                        .hasGeneratedReference("pets", "Pet")
                        .hasNoMoreFeatures(); // no reference for Shelter
                }
            )
            .hasTargetClass("Doggo", doggo -> {})
            .hasTargetClass(
                "Pet", pet -> {
                    assertThat(pet)
                        .isNotAbstract() // abstractness of the source class is not carried over
                        .sourceIs(lookup("kennel", "Animal"))
                        .sourceExcludesSubClasses()
                        .hasNoSuperClasses();
                }
            )
            .hasTargetClass(
                "Shelter", shelter -> {
                    assertThat(shelter)
                        .isAbstract()
                        .hasNoSource()
                        .hasNoSuperClasses()
                        .hasAbstractAttribute(
                            "label", "EString", options -> {
                                assertThat(options).hasBounds(1, 1).otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasAbstractAttribute(
                            "b", "EInt", options -> {
                                assertThat(options)
                                    .hasBounds(0, ETypedElement.UNBOUNDED_MULTIPLICITY)
                                    .isNot(Mod.Ordered)
                                    .otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasAbstractAttribute(
                            "c", "EString", options -> {
                                assertThat(options)
                                    .hasBounds(0, 1)
                                    .is(Mod.Volatile)
                                    .is(Mod.Transient)
                                    .is(Mod.Unsettable)
                                    .is(Mod.Derived)
                                    .isNot(Mod.Changeable)
                                    .is(Mod.Id)
                                    .otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasAbstractAttribute(
                            "d", "EInt", options -> {
                                assertThat(options)
                                    .hasBounds(2, 5)
                                    .isNot(Mod.Unique)
                                    .otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasAbstractReference(
                            "mascot", "Doggo", options -> {
                                assertThat(options)
                                    .hasBounds(1, 1)
                                    .is(Mod.Containment)
                                    .otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasAbstractReference(
                            "residents", "Doggo", options -> {
                                assertThat(options)
                                    .hasBounds(1, ETypedElement.UNBOUNDED_MULTIPLICITY)
                                    .isNot(Mod.Ordered)
                                    .otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasAbstractReference("parent", "Shelter", options -> assertThat(options).hasBounds(0, 1))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

}
