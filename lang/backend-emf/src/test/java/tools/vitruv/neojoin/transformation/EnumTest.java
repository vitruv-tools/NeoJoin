package tools.vitruv.neojoin.transformation;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.transformation.InstanceModelAssertions.assertThat;

public class EnumTest extends DefaultTransformationTest {

    @Test
    void enumToString() {
        var result = transform("""
            from Food f
            create Food {
                name = f.name
                typeName := f.type.name
            }
            """);

        assertThat(result)
            .hasInstance(
                "Food", named("Fanta"), foods -> {
                    assertThat(foods)
                        .hasAttribute("typeName", "DRINK");
                }
            )
            .hasInstance(
                "Food", named("Maultaschen"), foods -> {
                    assertThat(foods)
                        .hasAttribute("typeName", "FOOD");
                }
            )
            .hasInstance(
                "Food", named("Pizza Margherita"), foods -> {
                    assertThat(foods)
                        .hasAttribute("typeName", "FOOD");
                }
            )
            .hasNoMoreInstances();
    }

    @Test
    void enumToInt() {
        var result = transform("""
            from Food f
            create Food {
                name = f.name
                typeValue := f.type.value
            }
            """);

        assertThat(result)
            .hasInstance(
                "Food", named("Fanta"), foods -> {
                    assertThat(foods)
                        .hasAttribute("typeValue", 1);
                }
            )
            .hasInstance(
                "Food", named("Maultaschen"), foods -> {
                    assertThat(foods)
                        .hasAttribute("typeValue", 0);
                }
            )
            .hasInstance(
                "Food", named("Pizza Margherita"), foods -> {
                    assertThat(foods)
                        .hasAttribute("typeValue", 0);
                }
            )
            .hasNoMoreInstances();
    }

}
