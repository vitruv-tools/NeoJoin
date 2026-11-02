package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class AbstractClassParseTest extends AbstractParseTest {

    @Test
    void containmentOnAttribute() {
        var result = parse("""
            abstract A {
                x: EString [containment]
            }
            """);

        assertThat(result).hasExactlyIssues("Modifier 'containment' is not applicable to attributes");
    }

    @Test
    void duplicatedFeatureName() {
        var result = parse("""
            abstract A {
                x: EString
                x: EInt
            }
            """);

        assertThat(result).hasExactlyIssues("Duplicated feature name: x");
    }

    @Test
    void unknownFeatureType() {
        var result = parse("""
            abstract A {
                x: Unknown
            }
            """);

        assertThat(result).hasExactlyIssues("Unknown cannot be resolved.");
    }

    @Test
    void duplicatedClassName() {
        var result = parse("""
            abstract A {}

            create A {
                x := 1
            }
            """);

        assertThat(result).hasExactlyIssues("Duplicated target class named 'A'");
    }

    @Test
    void withoutBody() {
        var result = parse("""
            abstract A
            """);

        assertThat(result).hasExactlyIssues("mismatched input '<EOF>' expecting '{'");
    }

    @Test
    void withSource() {
        var result = parse("""
            from Food abstract A {}
            """);

        assertThat(result).hasExactlyIssues("missing 'create' at 'abstract'");
    }

}
