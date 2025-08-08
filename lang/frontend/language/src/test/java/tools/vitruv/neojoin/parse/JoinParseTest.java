package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class JoinParseTest extends AbstractParseTest {

    @Test
    void joinWithoutBody() {
        var result = parse("""
            from Restaurant join Food create
            """);

        assertThat(result)
            .hasIssues("Query with a join must have a body");
    }

    @Test
    void joinWithAliasCollision() {
        var result = parse("""
            from Restaurant r
            join Food r
            create {}
            """);

        assertThat(result)
            .hasIssues("Duplicated alias: r");
    }

    @Test
    void joinWithCondition() {
        var result = parse("""
            from Restaurant r
            join ReviewPage rv
                with r using name
            create {}
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void joinWithConditionOnReference() {
        var result = parse("""
            from Restaurant r1
            join Restaurant r2
                with r1 using sells
            create {}
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void joinWithMultiCondition() {
        var result = parse("""
            from Restaurant r
            join ReviewPage rv
                with r using name, name
            create {}
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void joinWithConditionWithoutWith() {
        var result = parse("""
            from Restaurant r
            join ReviewPage rv
                using name
            create {}
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void multiJoinWithConditionWithoutWith() {
        var result = parse("""
            from Restaurant r
            join ReviewPage rv
            join Food f
                using name
            create {}
            """);

        assertThat(result)
            .hasIssues("Join condition in a query with multiple joins must specify which other class to join on");
    }

    @Test
    void joinConditionWithUnknownOther() {
        var result = parse("""
            from Restaurant r
            join ReviewPage rv
                with f using name
            create {}
            """);

        assertThat(result)
            .hasIssues("f cannot be resolved.", "name cannot be resolved.");
    }

    @Test
    void joinConditionParamsAccessVisible() {
        var result = parse("""
            from Restaurant r
            join ReviewPage rv
                on rv.name == r.name
            join Food f
            create {}
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void joinConditionParamsAccessInvisible() {
        var result = parse("""
            from Restaurant r
            join ReviewPage rv
                on rv.name == f.name
            join Food f
            create {}
            """);

        assertThat(result)
            .hasIssues("The method or field f is undefined");
    }

}
