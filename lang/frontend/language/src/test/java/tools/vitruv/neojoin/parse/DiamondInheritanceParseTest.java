package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

/**
 * A feature that is inherited from several super classes is valid if the provided features are equivalent (same type
 * and effective modifiers) and an error if they conflict.
 */
public class DiamondInheritanceParseTest extends AbstractParseTest {

    @Test
    void equivalentFeaturesOverriddenOnBothPaths() {
        var result = parse("""
            abstract A {
                x: EString
            }

            abstract B extends A {
                x: EString
            }

            abstract C extends A {
                x: EString
            }

            from Restaurant r create D extends B, C {
                x = r.name
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void conflictingTypes() {
        var result = parse("""
            abstract A {
                x: EString
            }

            abstract B {
                x: EInt
            }

            from Restaurant r create C extends A, B {
                x = r.name
            }
            """);

        assertThat(result).hasExactlyIssues(
            "Feature 'x' is inherited from multiple super classes with different types or modifiers: A, B"
        );
    }

    @Test
    void conflictingModifiers() {
        var result = parse("""
            abstract A {
                x: EString [*]
            }

            abstract B {
                x: EString [+]
            }

            from Restaurant r create C extends A, B {
                x := #[r.name]
            }
            """);

        assertThat(result).hasExactlyIssues(
            "Feature 'x' is inherited from multiple super classes with different types or modifiers: A, B"
        );
    }

    @Test
    void effectiveModifiersOfOverrideAreCompared() {
        var result = parse("""
            abstract A {
                x: EString [+]
            }

            abstract B extends A {
                x: EString
            }

            abstract C {
                x: EString [+]
            }

            from Restaurant r create D extends B, C {
                x := #[r.name]
            }
            """);

        assertThat(result).hasNoIssues();
    }

}
