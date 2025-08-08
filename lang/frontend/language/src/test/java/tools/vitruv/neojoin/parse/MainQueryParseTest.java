package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class MainQueryParseTest extends AbstractParseTest {

    @Test
    void withoutTargetName() {
        var result = parse("""
            from Restaurant create {}
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void withTargetName() {
        var result = parse("""
            from Restaurant create Rest {}
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void duplicateTargetName() {
        var result = parse("""
            from Restaurant create Rest {}
            from Food create Rest {}
            """);

        assertThat(result).hasIssues("Duplicated target class named 'Rest'");
    }

    @Test
    void duplicateTargetNameImplicit() {
        var result = parse("""
            from Restaurant create {}
            from Food create Restaurant {}
            """);

        assertThat(result).hasIssues("Duplicated target class named 'Restaurant'");
    }

    @Test
    void queryWithoutFeatures() {
        var result = parse("""
            from Food create
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void queryWithoutHeaderDoesNotCrash() {
        var result = internalParse("""
            from Restaurant create
            """);

        assertThat(result)
            .hasIssues("mismatched input 'from' expecting 'export'", "Restaurant cannot be resolved.");
    }

    @Test
    void queryWithNoSource() {
        var result = parse("""
            create Result {
                test := 5 + 7
                test2: Food := null
            }
            
            from Food create
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void queryWithoutSourceAndWithoutName() {
        var result = parse("""
            create {}
            """);

        assertThat(result)
            .hasIssues("Query without source must have a target name");
    }

    @Test
    void queryWithoutSourceAndWithoutBody() {
        var result = parse("""
            create Result
            """);

        assertThat(result)
            .hasIssues("Query without source must have a body");
    }

    @Test
    void referenceToQueryWithoutSource() {
        var result = parse("""
            from Restaurant r create {
                test: Result := r
            }
            
            create Result {}
            """);

        assertThat(result)
            .hasIssues("Type mismatch: cannot convert from Restaurant to Result");
    }

}
