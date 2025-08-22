package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class RootParseTest extends AbstractParseTest {

    @Test
    void simple() {
        var result = parse("""
            create root Root {
                test := 5
            }
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void noName() {
        var result = parse("""
            create root {}
            """);

        assertThat(result)
            .hasIssues("Query without source must have a target name");
    }

    @Test
    void noBody() {
        var result = parse("""
            create root Root
            """);

        assertThat(result)
            .hasIssues("Query without source must have a body");
    }

    @Test
    void featureNameConflict() {
        var result = parse("""
            from Food create

            create root Rooty {
                allFoods := 5
            }
            """);

        assertThat(result)
            .hasIssues("Feature name collides with implicit root containment reference.");
    }

    @Test
    void multiRoot() {
        var result = parse("""
            create root Root {}
            create root Root2 {}
            """);

        assertThat(result)
            .hasIssues("Multiple root queries");
    }

    @Test
    void implicitRootNameCollision() {
        var result = parse("""
            create Root {}
            """);

        assertThat(result)
            .hasIssues("Target class name 'Root' collides with name of the implicit root class. " +
                "Either choose a different name for this class or explicitly create a root class with a different name.");
    }

    @Test
    void implicitRootNameCollisionWithImplicitName() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://vitruv.tools/cyclic"

            from Root create {}
            """);

        assertThat(result)
            .hasIssues("Target class name 'Root' collides with name of the implicit root class. " +
                "Either choose a different name for this class or explicitly create a root class with a different name.");
    }

}
