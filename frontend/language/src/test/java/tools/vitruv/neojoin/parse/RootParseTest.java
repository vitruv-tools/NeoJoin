package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

class RootParseTest extends AbstractParseTest {

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
            .hasExactlyIssues("Query without source must have a target name");
    }

    @Test
    void noBody() {
        var result = parse("""
            create root Root
            """);

        assertThat(result)
            .hasExactlyIssues("Query without source must have a body");
    }

    @Test
    void featureNameConflict() {
        var result = parse("""
            from Food create

            create root Rooty {
                foods := 5
            }
            """);

        assertThat(result)
            .hasExactlyIssues("Feature name collides with implicit root containment reference.");
    }

    @Test
    void featureNameConflictWithReferenceToImplicitClass() {
        var result = parse("""
            from Restaurant r create {
                r.sells
            }

            create root Rooty {
                foods := 5
            }
            """);

        var message = "Feature name collides with implicit root containment reference.";
        assertThat(result)
            .hasIssueAt(message, "foods")
            .hasExactlyIssues(message);
    }

    @Test
    void featureWithReferenceNameOfRootClassItselfIsAllowed() {
        var result = parse("""
            create root Main {
                mains := 5
            }
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void featureWithReferenceNameOfAbstractClassIsAllowed() {
        var result = parse("""
            abstract Item {
                name: EString
            }

            create root Main {
                items := 5
            }
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void namesDifferingOnlyInCaseOfFirstLetterCollideInRootClass() {
        var result = parse("""
            from Restaurant r create {
                r.sells
            }

            create food {}
            """);

        var message = "Target classes 'Food', 'food' have the same containment reference 'foods' in the root class. "
            + "Choose names that differ in more than the case of the first letter.";
        assertThat(result)
            .hasIssueAt(message, "r.sells", "food")
            .hasExactlyIssues(message);
    }

    @Test
    void abstractClassNeedsNoDistinctRootReferenceName() {
        var result = parse("""
            abstract food {}

            create Food {}
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void explicitRootClassNeedsNoDistinctRootReferenceName() {
        var result = parse("""
            create root food {}

            create Food {}
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void multiRoot() {
        var result = parse("""
            create root Root {}
            create root Root2 {}
            """);

        assertThat(result)
            .hasExactlyIssues("Multiple root queries");
    }

    @Test
    void implicitRootNameCollision() {
        var result = parse("""
            create Root {}
            """);

        assertThat(result)
            .hasExactlyIssues("Target class name 'Root' collides with name of the implicit root class. "
                + "Either choose a different name for this class "
                + "or explicitly create a root class with a different name.");
    }

    @Test
    void implicitRootNameCollisionWithImplicitName() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://vitruv.tools/cyclic"

            from Root create {}
            """);

        assertThat(result)
            .hasExactlyIssues("Target class name 'Root' collides with name of the implicit root class. "
                + "Either choose a different name for this class "
                + "or explicitly create a root class with a different name.");
    }

    @Test
    void implicitRootNameCollisionWithImplicitClass() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://example.org/menu"

            from Menu create
            """);

        var message = "Implicit class of source class 'Root' collides with name of the implicit root class. "
            + "Either create a query for the source class that specifies a different target class name "
            + "or explicitly create a root class with a different name.";
        assertThat(result)
            .hasIssueAt(message, "from Menu create")
            .hasExactlyIssues(message);
    }

}
