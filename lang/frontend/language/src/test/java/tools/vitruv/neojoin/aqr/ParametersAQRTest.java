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

}