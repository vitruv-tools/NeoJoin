package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

class MainQueryParseTest extends AbstractParseTest {

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
    void condition() {
        var result = parse("""
            from Restaurant r
            where r.name.startsWith("Pizzeria")
            create {}
            """);

        assertThat(result).hasNoIssues();
    }
    
    @Test
    void multipleConditions() {
        var result = parse("""
            from Restaurant r
            where r.name.startsWith("Pizzeria")
            where r.name.endsWith("Toni")
            create {}
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void nonBooleanCondition() {
        var result = parse("""
            from Restaurant r
            where r.name
            create {}
            """);

        assertThat(result).hasExactlyIssues("Type mismatch: cannot convert from String to boolean");
    }

    @Test
    void nonBooleanAdditionalCondition() {
        var result = parse("""
            from Restaurant r
            where r.name.startsWith("Pizzeria")
            where r.name
            create {}
            """);

        assertThat(result).hasExactlyIssues("Type mismatch: cannot convert from String to boolean");
    }

    @Test
    void duplicateTargetName() {
        var result = parse("""
            from Restaurant create Rest {}
            from Food create Rest {}
            """);

        assertThat(result).hasExactlyIssues("Duplicated target class named 'Rest'");
    }

    @Test
    void duplicateTargetNameImplicit() {
        var result = parse("""
            from Restaurant create {}
            from Food create Restaurant {}
            """);

        assertThat(result).hasExactlyIssues("Duplicated target class named 'Restaurant'");
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
            .hasExactlyIssues("mismatched input 'from' expecting 'export'", "Restaurant cannot be resolved.");
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
            .hasExactlyIssues("Query without source must have a target name");
    }

    @Test
    void queryWithoutSourceAndWithoutBody() {
        var result = parse("""
            create Result
            """);

        assertThat(result)
            .hasExactlyIssues("Query without source must have a body");
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
            .hasExactlyIssues("Type mismatch: cannot convert from Restaurant to Result");
    }

    /**
     * {@code create} followed by {@code abstract} is a concrete query without name and body, followed by an abstract
     * query. The correct syntax is {@code abstract A { ... }}.
     */
    @Test
    void createWithoutSourceNameAndBodyIsRejected() {
        var result = parse("""
            create abstract A {
                x: EString
            }
            """);

        assertThat(result).hasExactlyIssues("Incomplete query: 'create' needs a source, or a target name and a body");
    }

}
