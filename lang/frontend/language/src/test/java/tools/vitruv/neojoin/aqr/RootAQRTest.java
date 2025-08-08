package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.Constants;

import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

public class RootAQRTest extends AbstractAQRTest {

    @Test
    void implicit() {
        var aqr = parse("");

        assertThat(aqr)
            .hasRootTargetClass(
                Constants.DefaultRootClassName, root -> {
                    assertThat(root)
                        .hasNoCondition()
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void simple() {
        var aqr = parse("""
            create root Rooty {}
            """);

        assertThat(aqr)
            .hasRootTargetClass(
                "Rooty", root -> {
                    assertThat(root)
                        .hasNoCondition()
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void withCustomFeatures() {
        var aqr = parse("""
            create root Rooty {
                test := 5
            }
            """);

        assertThat(aqr)
            .hasRootTargetClass(
                "Rooty", root -> {
                    assertThat(root)
                        .hasNoCondition()
                        .hasCalculatedAttribute("test", "EInt")
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void rootReferences() {
        var aqr = parse("""
            from Restaurant create Restauranty
            
            create root Rooty {
                test := 5
            }
            """);

        assertThat(aqr)
            .hasRootTargetClass(
                "Rooty", root -> {
                    assertThat(root)
                        .hasNoCondition()
                        .hasCalculatedAttribute("test", "EInt")
                        .hasGeneratedReference("allRestaurantys", "Restauranty") // explicit
                        .hasGeneratedReference("allFoods", "Food") // implicit
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass("Restauranty", restaurant -> {})
            .hasTargetClass("Food", food -> {})
            .hasNoMoreTargetClasses();
    }

    @Test
    void withSource() {
        var aqr = parse("""
            from rest.Store create root Rooty {}
            """);

        assertThat(aqr)
            .hasRootTargetClass(
                "Rooty", root -> {
                    assertThat(root)
                        .hasNoCondition()
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

}
