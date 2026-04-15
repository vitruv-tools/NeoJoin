package tools.vitruv.neojoin.aqr;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.aqr.AQRAssertions.assertThat;

public class AQRParametersTest extends AbstractAQRTest {

    @Test
    void singleParameter() {
        var aqr = parse("""
            param minPrice : EInt
            """);

        assertThat(aqr)
            .hasParameter("minPrice", "EInt");
    }

    @Test
    void multipleParameter() {
        var aqr = parse("""
            param minPrice : EInt
            param name : EString
            """);

        assertThat(aqr)
            .hasParameter("minPrice", "EInt")
            .hasParameter("name", "EString");
    }

}