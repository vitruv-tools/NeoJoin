package tools.vitruv.neojoin.aqr;

import org.eclipse.emf.ecore.ETypedElement;
import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.aqr.AQRFeatureOptionsAssertions.Mod;

import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRFeatureOptionsAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

public class FeatureModifierAQRTest extends AbstractAQRTest {

    private static final int Unbounded = ETypedElement.UNBOUNDED_MULTIPLICITY;

    interface MultiplicityChecker {

        void check(String fieldName, int lowerBound, int upperBound);

    }

    @Test
    void multiplicity() {
        var aqr = parse("""
            from Restaurant r create Test {
                test1 [?] := r
                test2 [1] := r
                test3 [*] := #[r]
                test4 [+] := #[r]
                test5 [5] := #[r]
                test6 [5..*] := #[r]
                test7 [5..10] := #[r]
            }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Test", test -> {
                    MultiplicityChecker checker = (fieldName, lowerBound, upperBound) -> {
                        assertThat(test)
                            .hasCalculatedReference(
                                fieldName, "Test", options -> {
                                    assertThat(options).hasBounds(lowerBound, upperBound);
                                }
                            );
                    };

                    checker.check("test1", 0, 1);
                    checker.check("test2", 1, 1);
                    checker.check("test3", 0, Unbounded);
                    checker.check("test4", 1, Unbounded);
                    checker.check("test5", 5, 5);
                    checker.check("test6", 5, Unbounded);
                    checker.check("test7", 5, 10);
                }
            );
    }

    @Test
    void basic() {
        var aqr = parse("""
            from Restaurant r
            create Rest {
                name [volatile, !changeable] := r.name
            }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Rest", rest -> {
                    assertThat(rest)
                        .hasCalculatedAttribute(
                            "name", "EString", options -> {
                                assertThat(options)
                                    .is(Mod.Volatile)
                                    .isNot(Mod.Changeable)
                                    .otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void overwriteCopy() {
        var aqr = parse("""
            from Restaurant r
            create Rest {
                name1 [!changeable] := r.name
                name2 [!changeable] = r.name
                sells [!changeable, 3..7] = r.sells
            }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Rest", rest -> {
                    assertThat(rest)
                        .hasCalculatedAttribute(
                            "name1", "EString", options -> {
                                assertThat(options)
                                    .isNot(Mod.Changeable)
                                    .hasBounds(0, 1)
                                    .otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasCopiedAttribute(
                            "name2", lookup("restaurant", "Restaurant", "name"), options -> {
                                assertThat(options)
                                    .isNot(Mod.Changeable)
                                    .hasBounds(1, 1)
                                    .otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasCopiedReference(
                            "sells", "Food", lookup("restaurant", "Restaurant", "sells"), options -> {
                                assertThat(options)
                                    .isNot(Mod.Changeable)
                                    .hasBounds(3, 7)
                                    .otherModifiersHaveDefaultValue();
                            }
                        )
                        .hasNoMoreFeatures();
                }
            );
    }

}
