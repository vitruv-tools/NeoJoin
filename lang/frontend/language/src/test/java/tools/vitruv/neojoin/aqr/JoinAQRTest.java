package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;
import static tools.vitruv.neojoin.aqr.AQRTargetClassAssertions.assertThat;

public class JoinAQRTest extends AbstractAQRTest {

    @Test
    void basicJoinWithAttributes() {
        var aqr = parse("""
            from Restaurant rest
            join ReviewPage rev
            create ReviewedRestaurant {
                label = rev.name
                address = rest.address
            }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "ReviewedRestaurant", rest -> {
                    assertThat(rest)
                        .sourceIs(lookup("restaurant", "Restaurant"), "rest")
                        .hasJoin(lookup("reviewpage", "ReviewPage"), "rev", AQRJoin.Type.Inner)
                        .hasNoMoreJoins()
                        .hasNoCondition()
                        .hasCopiedAttribute("label", lookup("reviewpage", "ReviewPage", "name"))
                        .hasCopiedAttribute("address", lookup("restaurant", "Restaurant", "address"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void basicJoinWithReference() {
        var aqr = parse("""
            from Restaurant rest
            join ReviewPage rev
            create ReviewedRestaurant {
                rest.name
                revs = rev.reviews
            }
            """);

        assertThat(aqr)
            .hasTargetClass(
                "ReviewedRestaurant", rest -> {
                    assertThat(rest)
                        .sourceIs(lookup("restaurant", "Restaurant"), "rest")
                        .hasJoin(lookup("reviewpage", "ReviewPage"), "rev", AQRJoin.Type.Inner)
                        .hasNoMoreJoins()
                        .hasNoCondition()
                        .hasCopiedAttribute("name", lookup("restaurant", "Restaurant", "name"))
                        .hasCopiedReference("revs", "Review", lookup("reviewpage", "ReviewPage", "reviews"))
                        .hasNoMoreFeatures();
                }
            )
            .hasTargetClass(
                "Review", review -> {
                    assertThat(review)
                        .hasNoMoreJoins()
                        .hasNoCondition()
                        .hasCopiedAttribute("user", lookup("reviewpage", "Review", "user"))
                        .hasCopiedAttribute("rating", lookup("reviewpage", "Review", "rating"))
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void joinWithoutTargetName() {
        var aqr = parse("""
            from Restaurant rest
            join ReviewPage rev
            create {}
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Restaurant", rest -> {
                    assertThat(rest)
                        .sourceIs(lookup("restaurant", "Restaurant"))
                        .hasJoin(lookup("reviewpage", "ReviewPage"), "rev", AQRJoin.Type.Inner)
                        .hasNoMoreJoins()
                        .hasNoCondition()
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

    @Test
    void joinType() {
        var aqr = parse("""
            from Restaurant rest
            left join ReviewPage rev
            create {}
            """);

        assertThat(aqr)
            .hasTargetClass(
                "Restaurant", rest -> {
                    assertThat(rest)
                        .sourceIs(lookup("restaurant", "Restaurant"))
                        .hasJoin(lookup("reviewpage", "ReviewPage"), "rev", AQRJoin.Type.Left)
                        .hasNoMoreJoins()
                        .hasNoCondition()
                        .hasNoMoreFeatures();
                }
            )
            .hasNoMoreTargetClasses();
    }

}
