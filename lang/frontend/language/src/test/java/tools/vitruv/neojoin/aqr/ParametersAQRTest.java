package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;

public class ParametersAQRTest extends AbstractAQRTest {

    @Test
    void singleEDataTypeParameter() {
        var aqr = parse("""
            param minPrice : EInt
            """);

        assertThat(aqr)
            .hasParameter("minPrice", "EInt", false);
    }

    @Test
    void singleEClassParameter() {
        var aqr = parse("""
            param food : Food
            """);

        assertThat(aqr)
            .hasParameter("food", "Food", false);
    }

    @Test
    void multipleParameter() {
        var aqr = parse("""
            param minPrice : EInt
            param name : EString
            param food : Food
            param listFoods : EList<Food>
            """);

        assertThat(aqr)
            .hasParameter("minPrice", "EInt", false)
            .hasParameter("name", "EString", false)
            .hasParameter("food", "Food", false)
            .hasParameter("listFoods", "Food", true);
    }

        @Test
    void listEClassParameter() {
        var aqr = parse("""
            param food : EList<Food>
            """);

        assertThat(aqr)
            .hasParameter("food", "Food", true);
    }

    @Test
    void parameterUsedInJoinCondition() {
        var aqr = parse("""
            param nameFilter : EString
            from Restaurant rest
            join ReviewPage rev on rev.name == nameFilter
            create ReviewedRestaurant {}
            """);

        assertThat(aqr)
            .hasParameter("nameFilter", "EString", false);
    }

    @Test
    void parameterUsedInGroupBy() {
        var aqr = parse("""
            param groupKey : EString
            from Restaurant r
            group by groupKey
            create Test {}
            """);

        assertThat(aqr)
            .hasParameter("groupKey", "EString", false);
    }

    @Test
    void parameterUsedAsFeatureValue() {
        var aqr = parse("""
            param label : EString
            from Restaurant r
            create Rest {
                name := label
            }
            """);

        assertThat(aqr)
            .hasParameter("label", "EString", false);
    }

}