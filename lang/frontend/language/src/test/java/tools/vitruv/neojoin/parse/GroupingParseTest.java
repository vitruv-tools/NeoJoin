package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class GroupingParseTest extends AbstractParseTest {

    @Test
    void queryWithSingleAggregation() {
        var result = parse("""
            from Restaurant r
            group by r.name
            create {
                name := r.first?.name
            }
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void queryWithMultiAggregation() {
        var result = parse("""
            from Restaurant r
            group by r.name, r.numEmployees
            create {
                name := r.first?.name
            }
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void queryWithAggregationButWithoutBody() {
        var result = parse("""
            from Restaurant r
            group by r.name
            create
            """);

        assertThat(result)
            .hasIssues("Query with aggregation must have a body");
    }

}
