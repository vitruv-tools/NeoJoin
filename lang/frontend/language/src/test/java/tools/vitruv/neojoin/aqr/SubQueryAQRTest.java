package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

public class SubQueryAQRTest extends AbstractAQRTest {

    @Test
    void simple() {
        var aqr = parse("""
            from Restaurant
            create {
                it.name
                it.sells create {
                    it.price
                }
            }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Restaurant", rest -> {
                    assertThat(rest)
                        .sourceIs(lookup("restaurant", "Restaurant"))
                        .hasNoCondition()
                        .hasCopiedAttribute("name", lookup("restaurant", "Restaurant", "name"))
                        .hasCopiedReference("sells", "Food", lookup("restaurant", "Restaurant", "sells"))
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "Food", food -> {
                    assertThat(food)
                        .sourceIs(lookup("restaurant", "Food"))
                        .hasNoCondition()
                        .hasCopiedAttribute("price", lookup("restaurant", "Food", "price"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void renamed() {
        var aqr = parse("""
            from Restaurant
            create {
                it.name
                it.sells create TastyFood {
                    it.price
                }
            }
            """);

        assertThat(aqr)
            .hasTargetClass("Restaurant", rest -> {})
            .hasTargetClass(
                "TastyFood", food -> {
                    assertThat(food)
                        .sourceIs(lookup("restaurant", "Food"))
                        .hasNoCondition()
                        .hasCopiedAttribute("price", lookup("restaurant", "Food", "price"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void renamedWithoutBody() {
        var aqr = parse("""
            from Restaurant
            create {
                it.name
                it.sells create TastyFood
            }
            """);

        assertThat(aqr)
            .hasTargetClass("Restaurant", rest -> {})
            .hasTargetClass(
                "TastyFood", food -> {
                    assertThat(food)
                        .sourceIs(lookup("restaurant", "Food"))
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
    void multi() {
        var aqr = parse("""
            from ReviewPage revPage
            create Test {
                r1 = revPage.reviews create A { it.user }
                r2 = revPage.reviews create B { it.user }
            }
            """);

        assertThat(aqr)
            .hasTargetClass("Test", (c) -> {})
            .hasTargetClass("A", a -> assertThat(a).sourceIs(lookup("reviewpage", "Review")))
            .hasTargetClass("B", b -> assertThat(b).sourceIs(lookup("reviewpage", "Review")))
            .hasNoMoreTargetClasses();
    }

}
