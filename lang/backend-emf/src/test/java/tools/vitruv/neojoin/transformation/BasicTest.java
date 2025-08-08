package tools.vitruv.neojoin.transformation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tools.vitruv.neojoin.transformation.InstanceModelAssertions.assertThat;

public class BasicTest extends DefaultTransformationTest {

    @Test
    void basic() {
        var result = transform("""
            from Restaurant r create { r.name }
            """);

        assertThat(result)
            .hasInstance("Restaurant", named("Pizzeria Toni"))
            .hasInstance("Restaurant", named("Brauhaus"))
            .hasNoMoreInstances();
    }

    @Test
    void condition() {
        var result = transform("""
            from Restaurant r where r.name.startsWith("Pizzeria") create { r.name }
            """);

        assertThat(result)
            .hasInstance("Restaurant", named("Pizzeria Toni"))
            .hasNoMoreInstances();
    }

    @Test
    void expression() {
        var result = transform("""
            from Restaurant r create {
                r.name
                len := r.name.length
            }
            """);

        assertThat(result)
            .hasInstance(
                "Restaurant", named("Pizzeria Toni"), rest -> {
                    assertThat(rest)
                        .hasAttribute("name", "Pizzeria Toni")
                        .hasAttribute("len", 13);
                }
            )
            .hasInstance(
                "Restaurant", named("Brauhaus"), rest -> {
                    assertThat(rest)
                        .hasAttribute("name", "Brauhaus")
                        .hasAttribute("len", 8);
                }
            )
            .hasNoMoreInstances();
    }

    @Test
    void multiClass() {
        var result = transform("""
            from Restaurant r create {
                r.name
            }
            
            from Food f create {
                f.name
            }
            """);

        assertThat(result)
            .hasInstance("Restaurant", named("Pizzeria Toni"))
            .hasInstance("Restaurant", named("Brauhaus"))
            .hasInstance("Food", named("Pizza Margherita"))
            .hasInstance("Food", named("Maultaschen"))
            .hasInstance("Food", named("Fanta"))
            .hasNoMoreInstances();
    }

    @Test
    void multiClassWithReference() {
        var result = transform("""
            from Restaurant r create {
                r.name
                r.sells
            }
            
            from Food f create {
                f.name
            }
            """);

        assertThat(result)
            .hasInstance(
                "Restaurant", named("Pizzeria Toni"), rest -> {
                    assertThat(rest)
                        .hasManyReference(
                            "sells", named("Pizza Margherita"), named("Fanta")
                        );
                }
            )
            .hasInstance(
                "Restaurant", named("Brauhaus"), rest -> {
                    assertThat(rest)
                        .hasManyReference("sells", named("Maultaschen"), named("Fanta"));
                }
            )
            .hasInstance("Food", named("Pizza Margherita"))
            .hasInstance("Food", named("Maultaschen"))
            .hasInstance("Food", named("Fanta"))
            .hasNoMoreInstances();
    }

    @Test
    void renamedTarget() {
        var result = transform("""
            from Restaurant r create {
                r.name
                r.sells
            }
            
            from Food f create TastyFood {
                f.name
            }
            """);

        assertThat(result)
            .hasInstance("Restaurant", named("Pizzeria Toni"))
            .hasInstance("Restaurant", named("Brauhaus"))
            .hasInstance("TastyFood", named("Pizza Margherita"))
            .hasInstance("TastyFood", named("Maultaschen"))
            .hasInstance("TastyFood", named("Fanta"))
            .hasNoMoreInstances();
    }

    @Test
    void missingTarget() {
        assertThatThrownBy(() -> {
            transform("""
                from Restaurant create
                
                from Food f
                where false
                create TastyFood {}
                """);
        }).hasMessage(
            "Failed to transform models: no target instance of class 'TastyFood' found for source instance of class 'Food'"
        );
    }

    @Test
    void ambiguousTarget() {
        assertThatThrownBy(() -> {
            transform("""
                from Restaurant create
                
                from Food f
                join Food f2
                create TastyFood {}
                """);
        }).hasMessage(
            "Failed to transform models: multiple target instances of class 'TastyFood' found for source instance of class 'Food'"
        );
    }

    @Test
    void indirectFeatureCopy() {
        transform("""
            from restaurant.Store s create Store {
                s.restaurants.first.sells
            }
            """);
    }

    @Test
    void notManyReference() {
        transform("""
            from Restaurant r create {
                r.name
                sells := r.sells.first
            }
            """);
    }

    @Test
    void selfReference() {
        var result = transform("""
            from Restaurant rest
            create Result {
                rest.name
                self := rest
            }
            """);

        assertThat(result)
            .hasInstance(
                "Result", named("Pizzeria Toni"), rest -> {
                    assertThat(rest)
                        .hasAttribute("name", "Pizzeria Toni")
                        .hasReference("self", other -> other == rest);
                }
            )
            .hasInstance(
                "Result", named("Brauhaus"), rest -> {
                    assertThat(rest)
                        .hasAttribute("name", "Brauhaus")
                        .hasReference("self", other -> other == rest);
                }
            )
            .hasNoMoreInstances();
    }

    @Test
    void nullValueInFeature() {
        var result = transform("""
            from Restaurant rest
            create Result {
                rest.name
                test := rest.sells.findFirst[false]
            }
            """);

        assertThat(result)
            .hasInstance(
                "Result", named("Pizzeria Toni"), rest -> {
                    assertThat(rest)
                        .hasReference("test", other -> other == null);
                }
            )
            .hasInstance(
                "Result", named("Brauhaus"), rest -> {
                    assertThat(rest)
                        .hasReference("test", other -> other == null);
                }
            );
    }

    @Test
    void noSourceTest() {
        var result = transform("""
            create Result {
                test := 5 + 7
            }
            """);

        assertThat(result)
            .hasInstance(
                "Result", any(), res -> {
                    assertThat(res)
                        .hasAttribute("test", 12);
                }
            )
            .hasNoMoreInstances();
    }

    @Test
    void manyAttribute() {
        var result = transform("""
            create Test {
                test [*] := #[1, 2]
            }
            """);

        assertThat(result)
            .hasInstance(
                "Test", any(), test -> {
                    assertThat(test)
                        .hasAttribute("test", List.of(1, 2));
                }
            )
            .hasNoMoreInstances();
    }

    @Test
    void containmentError() {
        assertThatThrownBy(() -> {
            transform("""
                from Restaurant r create {
                    sells [containment] := r.sells
                }
                """);
        }).hasMessage(
            "Failed to transform models: cannot add target instance of class 'Food' to containment reference 'Restaurant.sells' because it is already contained in another instance of class 'Restaurant'"
        );
    }

    @Test
    void subQuery() {
        var result = transform("""
            from Restaurant r
            where r.name.startsWith("Pizzeria")
            create {
                it.name
                it.sells create {
                    it.name
                    it.price
                }
            }
            """);

        assertThat(result)
            .hasInstance(
                "Restaurant", named("Pizzeria Toni"), rest -> {
                    assertThat(rest)
                        .hasAttribute("name", "Pizzeria Toni")
                        .hasManyReference("sells", named("Pizza Margherita"), named("Fanta"));
                }
            )
            .hasInstance("Food", named("Pizza Margherita"))
            .hasInstance("Food", named("Fanta"))
            .hasInstance("Food", named("Maultaschen"))
            .hasNoMoreInstances();
    }

}
