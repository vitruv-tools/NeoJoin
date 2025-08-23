package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;
import static tools.vitruv.neojoin.utils.Assertions.check;


class AQRTest extends AbstractAQRTest {

    @Test
    void simpleCopyClass() {
        var aqr = parse("""
            from Restaurant create {}
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Restaurant", restaurant -> {
                    assertThat(restaurant)
                        .sourceIs(lookup("restaurant", "Restaurant"))
                        .hasNoCondition()
                        .hasNoMoreFeatures();
                }
            );
    }

    @Test
    void simpleCopyAndRenameClass() {
        var aqr = parse("""
            from Restaurant create Rest {}
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Rest", rest -> {
                    assertThat(rest)
                        .sourceIs(lookup("restaurant", "Restaurant"))
                        .hasNoCondition()
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void noFeatureBlock() {
        var aqr = parse("""
            from Restaurant create Rest
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Food", food -> {
                    assertThat(food)
                        .sourceIs(lookup("restaurant", "Food"))
                        .hasNoCondition()
                        .hasCopiedAttribute("name", lookup("restaurant", "Food", "name"))
                        .hasCopiedAttribute("price", lookup("restaurant", "Food", "price"))
                        .hasCopiedAttribute("type", lookup("restaurant", "Food", "type"))
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "Rest", rest -> {
                    assertThat(rest)
                        .sourceIs(lookup("restaurant", "Restaurant"))
                        .hasNoCondition()
                        .hasCopiedAttribute("name", lookup("restaurant", "Restaurant", "name"))
                        .hasCopiedReference("sells", "Food", lookup("restaurant", "Restaurant", "sells"));
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void escaping() {
        var aqr = parse("""
            from Food ^f create ^TastyFood {
                ^name := f.name + ^f.name
            }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "TastyFood", food -> {
                    assertThat(food)
                        .sourceIs(lookup("restaurant", "Food"))
                        .hasNoCondition()
                        .hasCalculatedAttribute("name", "EString")
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void multipleTargets() {
        var aqr = parse("""
            from Food create TastyFood
            from Food create NotSoTastyFood
            """);

        assertThat(aqr)
            .hasTargetClass(
                "TastyFood", food -> {
                    assertThat(food)
                        .hasNoCondition()
                        .hasCopiedAttribute("name", lookup("restaurant", "Food", "name"))
                        .hasCopiedAttribute("price", lookup("restaurant", "Food", "price"))
                        .hasCopiedAttribute("type", lookup("restaurant", "Food", "type"))
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "NotSoTastyFood", food -> {
                    assertThat(food)
                        .hasNoCondition()
                        .hasCopiedAttribute("name", lookup("restaurant", "Food", "name"))
                        .hasCopiedAttribute("price", lookup("restaurant", "Food", "price"))
                        .hasCopiedAttribute("type", lookup("restaurant", "Food", "type"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void condition() {
        var aqr = parse("""
            from Restaurant r
            where r.name.startsWith("Pizzeria")
            create Rest {}
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Rest", rest -> {
                    assertThat(rest)
                        .hasCondition()
                        .hasNoMoreFeatures();
                }
            );
    }

    @Test
    void grouping() {
        var aqr = parse("""
            from Restaurant r
            group by r.name, r.numEmployees, r.name
            create Test {}
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Test", test -> {
                    check(test.source() != null);
                    assertThat(test.source().groupingExpressions()).hasSize(3);
                }
            );
    }

    @Test
    void explicitTypeWithSubQueryWithImplicitName() {
        // see https://github.com/vitruv-tools/NeoJoin/issues/80
        parse("""
            from Food f create Dish {
                name: EString := f.name
            }

            from Restaurant r create {
                r.sells create {
                    it.name
                }
            }
            """);
    }

}
