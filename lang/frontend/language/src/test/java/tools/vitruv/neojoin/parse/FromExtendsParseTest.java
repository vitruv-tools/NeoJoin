package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class FromExtendsParseTest extends AbstractInheritanceParseTest {

    @Test
    void withoutAlias() {
        var result = parseWithKennel("""
            from extends Animal create
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void withAlias() {
        var result = parseWithKennel("""
            from extends Animal a create Pet {
                name = a.name
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void withJoin() {
        var result = parseWithKennel("""
            from extends Animal a
            join extends Dog d
                on a.name == d.breed
            create {
                a.name
            }
            """);

        assertThat(result).hasNoIssues();
    }

}
