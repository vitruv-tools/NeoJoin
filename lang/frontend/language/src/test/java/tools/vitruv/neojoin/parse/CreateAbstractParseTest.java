package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class CreateAbstractParseTest extends AbstractInheritanceParseTest {

    /**
     * {@code create} followed by {@code abstract} is a concrete query without name and body, followed by an abstract
     * query. The correct syntax is {@code abstract A { ... }}.
     */
    @Test
    void createAbstractIsRejected() {
        var result = parse("""
            create abstract A {
                x: EString
            }
            """);

        assertThat(result).hasIssues("Incomplete query: 'create' needs a source, a target name or a body");
    }

    @Test
    void loneCreateIsRejected() {
        var result = parse("""
            create
            """);

        assertThat(result).hasIssues("Incomplete query: 'create' needs a source, a target name or a body");
    }

}
