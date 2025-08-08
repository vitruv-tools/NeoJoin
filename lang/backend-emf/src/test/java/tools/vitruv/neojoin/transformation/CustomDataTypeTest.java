package tools.vitruv.neojoin.transformation;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.transformation.InstanceModelAssertions.assertThat;

public class CustomDataTypeTest extends DefaultTransformationTest {

    @Test
    void copyDataType() {
        var result = transform("""
            from Restaurant r create {
                r.name
                r.dailyRevenue
            }
            """);

        assertThat(result)
            .hasInstance(
                "Restaurant", named("Pizzeria Toni"), rest -> {
                    assertThat(rest)
                        .hasAttribute("dailyRevenue", 5000.0f);
                }
            )
            .hasInstance(
                "Restaurant", named("Brauhaus"), rest -> {
                    assertThat(rest)
                        .hasAttribute("dailyRevenue", 7000.0f);
                }
            )
            .hasNoMoreInstances();
    }

    @Test
    void copyEnum() {
        var result = transform("""
            from Food f create {
                f.name
                f.type
            }
            """);

        assertThat(result)
            .hasInstance(
                "Food", named("Pizza Margherita"), rest -> {
                    assertThat(rest)
                        .hasAttribute("type", "FOOD");
                }
            )
            .hasInstance(
                "Food", named("Maultaschen"), rest -> {
                    assertThat(rest)
                        .hasAttribute("type", "FOOD");
                }
            )
            .hasInstance(
                "Food", named("Fanta"), rest -> {
                    assertThat(rest)
                        .hasAttribute("type", "DRINK");
                }
            )
            .hasNoMoreInstances();
    }

}
