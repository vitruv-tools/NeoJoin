package tools.vitruv.neojoin.transformation;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.transformation.InstanceModelAssertions.assertThat;

public class GroupingTest extends DefaultTransformationTest {

    @Test
    void allIntoOne() {
        var result = transform("""
            from Food f
            group by true
            create Foods {
                minPrice := f.map[ it.price ].min()
                maxPrice := f.map[ it.price ].max()
            }
            """);

        assertThat(result)
            .hasInstance(
                "Foods", any(), foods -> {
                    assertThat(foods)
                        .hasAttribute("minPrice", 5.0f)
                        .hasAttribute("maxPrice", 8.0f);
                }
            )
            .hasNoMoreInstances();
    }

    @Test
    void multipleResults() {
        var result = transform("""
            from Food f
            group by f.price < 6
            create Foods {
                name := f.map[ it.name ].sort.^join("+")
                minPrice := f.map[ it.price ].min()
                maxPrice := f.map[ it.price ].max()
            }
            """);

        assertThat(result)
            .hasInstance(
                "Foods", named("Maultaschen+Pizza Margherita"), foods -> {
                    assertThat(foods)
                        .hasAttribute("minPrice", 7.0f)
                        .hasAttribute("maxPrice", 8.0f);
                }
            )
            .hasInstance(
                "Foods", named("Fanta"), foods -> {
                    assertThat(foods)
                        .hasAttribute("minPrice", 5.0f)
                        .hasAttribute("maxPrice", 5.0f);
                }
            )
            .hasNoMoreInstances();
    }

    @Test
    void extensions() {
        var result = transform("""
            from Restaurant r
            left join Food f
                on r.sells.contains(f)
            group by r
            create Restaurant {
                name := r.map[ it.name ].same
                test := java.util.List.of("Test").single
            }
            """);

        assertThat(result)
            .hasInstance(
                "Restaurant", named("Pizzeria Toni"), rest -> {
                    assertThat(rest)
                        .hasAttribute("test", "Test");
                }
            )
            .hasInstance(
                "Restaurant", named("Brauhaus"), rest -> {
                    assertThat(rest)
                        .hasAttribute("test", "Test");
                }
            )
            .hasNoMoreInstances();
    }

}
