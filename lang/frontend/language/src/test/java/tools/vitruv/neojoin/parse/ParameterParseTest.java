package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class ParameterParseTest extends AbstractParseTest {

    @Test
    void singleEDataTypeParam() {
        var result = parse("""
            param minPrice : EInt
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void singleEClassParam() {
        var result = parse("""
            param someFood : Food
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void unknownEClassParam() {
        var result = parse("""
            param p : SomeUnknownClass
            """);

        assertThat(result)
            .hasIssues("SomeUnknownClass cannot be resolved.");
    }

    @Test
    void singleEClassWithAliasParam() {
        var result = parse("""
            param store : rest.Store
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void singleAmbiguousEClassParam() {
        var result = parse("""
            param store : Store
            """);

        assertThat(result)
            .hasIssues("Store cannot be resolved.");
    }

    @Test
    void multipleParams() {
        var result = parse("""
            param minPrice : EInt
            param name : EString
            param food : Food
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void listOfEDataTypeParam() {
        var result = parse("""
            param food : EList<EInt>
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void listOfEClassParam() {
        var result = parse("""
            param food : EList<Food>
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void listOfEClassWithAliasParam() {
        var result = parse("""
            param food : EList<rest.Food>
            """);

        assertThat(result)
            .hasNoIssues();
    }

}