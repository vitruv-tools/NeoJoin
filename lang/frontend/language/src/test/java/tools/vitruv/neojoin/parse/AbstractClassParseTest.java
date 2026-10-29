package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class AbstractClassParseTest extends AbstractParseTest {

    @Test
    void emptyBody() {
        var result = parse("""
            abstract A {}
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void features() {
        var result = parse("""
            abstract Shop {
                name: EString [?]
                revenue: Money
                sells: Food [+, containment]
                next: Shop [*]
            }

            from Food create
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void extendsAbstractClass() {
        var result = parse("""
            abstract A {
                x: EString
            }

            abstract B extends A {
                x: EString
                y: EInt
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void emptyModifierList() {
        var result = parse("""
            abstract A {
                x: EString []
            }
            """);

        assertThat(result).hasIssues("Empty modifier list");
    }

    @Test
    void containmentOnAttribute() {
        var result = parse("""
            abstract A {
                x: EString [containment]
            }
            """);

        assertThat(result).hasIssues("Modifier 'containment' is not applicable to attributes");
    }

    @Test
    void idOnReference() {
        var result = parse("""
            abstract A {
                x: B [id]
            }

            abstract B {}
            """);

        assertThat(result).hasIssues("Modifier 'id' is not applicable to references");
    }

    @Test
    void duplicatedFeatureName() {
        var result = parse("""
            abstract A {
                x: EString
                x: EInt
            }
            """);

        assertThat(result).hasIssues("Duplicated feature name: x");
    }

    @Test
    void unknownFeatureType() {
        var result = parse("""
            abstract A {
                x: Unknown
            }
            """);

        assertThat(result).hasIssues("Unknown cannot be resolved.");
    }

    @Test
    void duplicatedClassName() {
        var result = parse("""
            abstract A {}

            create A {
                x := 1
            }
            """);

        assertThat(result).hasIssues("Duplicated target class named 'A'");
    }

}
