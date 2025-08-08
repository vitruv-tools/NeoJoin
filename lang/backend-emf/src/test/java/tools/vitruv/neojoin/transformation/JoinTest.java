package tools.vitruv.neojoin.transformation;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.transformation.InstanceModelAssertions.assertThat;

@SuppressWarnings("unchecked")
public class JoinTest extends DefaultTransformationTest {

    @Test
    void cartesianProduct() {
        var result = transform("""
            from Restaurant rest
            join ReviewPage rev
            create ReviewedRestaurant {
                ^left = rest.name
                ^right = rev.name
            }
            """);

        assertThat(result)
            .hasInstance(
                "ReviewedRestaurant",
                and(attribute("left", "Pizzeria Toni"), attribute("right", "Pizzeria Toni"))
            )
            .hasInstance(
                "ReviewedRestaurant",
                and(attribute("left", "Pizzeria Toni"), attribute("right", "Brauhaus"))
            )
            .hasInstance(
                "ReviewedRestaurant",
                and(attribute("left", "Brauhaus"), attribute("right", "Pizzeria Toni"))
            )
            .hasInstance(
                "ReviewedRestaurant",
                and(attribute("left", "Brauhaus"), attribute("right", "Brauhaus"))
            )
            .hasNoMoreInstances();
    }

    @Test
    void joinOnAttribute() {
        var result = transform("""
            from Restaurant rest
            join ReviewPage rev
                using name
            create ReviewedRestaurant {
                ^left = rest.name
                ^right = rev.name
            }
            """);

        assertThat(result)
            .hasInstance(
                "ReviewedRestaurant",
                and(attribute("left", "Pizzeria Toni"), attribute("right", "Pizzeria Toni"))
            )
            .hasInstance(
                "ReviewedRestaurant",
                and(attribute("left", "Brauhaus"), attribute("right", "Brauhaus"))
            )
            .hasNoMoreInstances();
    }

    @Test
    void joinOnReference() {
        var result = transform("""
            from Restaurant r1
            join Restaurant r2
                using sells
            create ReviewedRestaurant {
                ^left = r1.name
                ^right = r2.name
            }
            """);

        assertThat(result)
            .hasInstance(
                "ReviewedRestaurant",
                and(attribute("left", "Pizzeria Toni"), attribute("right", "Pizzeria Toni"))
            )
            .hasInstance(
                "ReviewedRestaurant",
                and(attribute("left", "Brauhaus"), attribute("right", "Brauhaus"))
            )
            .hasNoMoreInstances();
    }

    @Test
    void multiJoin() {
        var result = transform("""
            from Restaurant rest
            join ReviewPage rev
                with rest using name
            join Food f
            where rest.sells.contains(f)
            create FoodWithRestaurant {
                restaurantName = rest.name
                reviewPageName = rev.name
                foodName = f.name
            }
            """);

        assertThat(result)
            .hasInstance(
                "FoodWithRestaurant",
                and(
                    attribute("restaurantName", "Pizzeria Toni"),
                    attribute("reviewPageName", "Pizzeria Toni"),
                    attribute("foodName", "Pizza Margherita")
                )
            )
            .hasInstance(
                "FoodWithRestaurant",
                and(
                    attribute("restaurantName", "Pizzeria Toni"),
                    attribute("reviewPageName", "Pizzeria Toni"),
                    attribute("foodName", "Fanta")
                )
            )
            .hasInstance(
                "FoodWithRestaurant",
                and(
                    attribute("restaurantName", "Brauhaus"),
                    attribute("reviewPageName", "Brauhaus"),
                    attribute("foodName", "Maultaschen")
                )
            )
            .hasInstance(
                "FoodWithRestaurant",
                and(
                    attribute("restaurantName", "Brauhaus"),
                    attribute("reviewPageName", "Brauhaus"),
                    attribute("foodName", "Fanta")
                )
            )
            .hasNoMoreInstances();
    }

    @Test
    void joinWithExpressionCondition() {
        var result = transform("""
            from Restaurant rest
            join ReviewPage rev
                on rev.name == rest.name
                on rev.name.startsWith("Brauhaus")
            create ReviewedRestaurant {
                ^left = rest.name
                ^right = rev.name
            }
            """);

        assertThat(result)
            .hasInstance(
                "ReviewedRestaurant",
                and(attribute("left", "Brauhaus"), attribute("right", "Brauhaus"))
            )
            .hasNoMoreInstances();
    }

    @Test
    void leftJoinTest() {
        var result = transform("""
            from Restaurant rest
            left join ReviewPage rev
                using name
                on rev.name.startsWith("Pizzeria")
            create ReviewedRestaurant {
                ^left = rest.name
                ^right = rev?.name
            }
            """);

        assertThat(result)
            .hasInstance(
                "ReviewedRestaurant",
                and(attribute("left", "Pizzeria Toni"), attribute("right", "Pizzeria Toni"))
            )
            .hasInstance(
                "ReviewedRestaurant",
                and(attribute("left", "Brauhaus"), attribute("right", null))
            )
            .hasNoMoreInstances();
    }

    @Test
    void joinOnNullObject() {
        var result = transform("""
            from Restaurant r1
            left join Restaurant r2
                on false
            left join Restaurant r3
                with r2 using name
            where r1.name == "Pizzeria Toni"
            create {
                name1 = r1.name
                name2 := r2?.name
                name3 := r3?.name
            }
            """);

        assertThat(result)
            .hasInstance(
                "Restaurant", any(), rest -> {
                    assertThat(rest)
                        .hasAttribute("name1", "Pizzeria Toni")
                        .hasAttribute("name2", null)
                        .hasAttribute("name3", null);
                }
            )
            .hasNoMoreInstances();
    }

}
