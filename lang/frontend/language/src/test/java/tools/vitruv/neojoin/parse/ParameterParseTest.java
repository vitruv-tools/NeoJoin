package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class ParameterParseTest extends AbstractParseTest {

    @Test
    void singleParam() {
        var result = parse("""
            param minPrice : EInt
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void multipleParams() {
        var result = parse("""
            param minPrice : EInt
            param name : EString
            """);

        assertThat(result)
            .hasNoIssues();
    }
}