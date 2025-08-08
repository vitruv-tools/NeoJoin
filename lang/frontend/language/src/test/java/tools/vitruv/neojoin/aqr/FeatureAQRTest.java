package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

public class FeatureAQRTest extends AbstractAQRTest {

    @Test
    void copyWithAttribute() {
        var aqr = parse("""
            from Restaurant r create Rest { r.name }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Rest", rest -> {
                    assertThat(rest)
                        .sourceIs(lookup("restaurant", "Restaurant"))
                        .hasNoCondition()
                        .hasCopiedAttribute("name", lookup("restaurant", "Restaurant", "name"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void copyWithRenamedAttribute() {
        var aqr = parse("""
            from Restaurant r create Rest { label = r.name }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Rest", rest -> {
                    assertThat(rest)
                        .sourceIs(lookup("restaurant", "Restaurant"))
                        .hasNoCondition()
                        .hasCopiedAttribute("label", lookup("restaurant", "Restaurant", "name"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void copyWithCalculatedAttribute() {
        var aqr = parse("""
            from Restaurant r create Rest { label := r.name }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Rest", rest -> {
                    assertThat(rest)
                        .sourceIs(lookup("restaurant", "Restaurant"))
                        .hasNoCondition()
                        .hasCalculatedAttribute("label", "EString")
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void copyClassWithReference() {
        var aqr = parse("""
            from Restaurant r create Rest { r.sells }
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
                        .hasCopiedReference("sells", "Food", lookup("restaurant", "Restaurant", "sells"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void implicitNullFeature() {
        var aqr = parse("""
            from Restaurant rest
            create Result {
                test := rest.sells.findFirst[false]
            }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Result", res -> {
                    assertThat(res)
                        .sourceIs(lookup("restaurant", "Restaurant"))
                        .hasNoCondition()
                        .hasCalculatedReference("test", "Food")
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass("Food", f -> {})
            .hasNoMoreTargetClasses();
    }

    @Test
    void explicitNullFeature() {
        var aqr = parse("""
            from Restaurant rest
            create Result {
                test: Food := null
            }
            
            from Food create
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Result", res -> {
                    assertThat(res)
                        .sourceIs(lookup("restaurant", "Restaurant"))
                        .hasNoCondition()
                        .hasCalculatedReference("test", "Food")
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass("Food", f -> {})
            .hasNoMoreTargetClasses();
    }

    @Test
    void ambiguousButExplicitTargetClass() {
        var result = parse("""
            from Restaurant r create {
                sells: NotSoTastyFood = r.sells
            }
            
            from Food create TastyFood
            from Food create NotSoTastyFood
            """);

        assertThat(result)
            .hasTargetClass(
                "Restaurant", rest -> {
                    assertThat(rest)
                        .hasCopiedReference("sells", "NotSoTastyFood", lookup("restaurant", "Restaurant", "sells"));
                }
            )
            .hasTargetClass(
                "TastyFood", food -> {
                    assertThat(food).sourceIs(lookup("restaurant", "Food"));
                }
            )
            .hasTargetClass(
                "NotSoTastyFood", food -> {
                    assertThat(food).sourceIs(lookup("restaurant", "Food"));
                }
            )
            .hasNoMoreTargetClasses();
    }

}
