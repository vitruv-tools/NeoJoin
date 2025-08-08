package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class FeatureModifierParseTest extends AbstractParseTest {

    @Test
    void noModifiers() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name = r.name
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void emptyModifiers() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [] = r.name
            }
            """);

        assertThat(result).hasIssues("Empty modifier list");
    }

    @Test
    void multiplicity() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [1] = r.name
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void singleModifier() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [!changeable] = r.name
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void negatedModifier() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [changeable] = r.name
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void multipleModifier() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [changeable, volatile, !transient] = r.name
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void modifierAndMultiplicity() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [changeable, volatile, !transient, 1] = r.name
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void duplicatedModifier() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [changeable, volatile, !changeable, 1] = r.name
            }
            """);

        assertThat(result).hasIssues("Duplicated modifier 'changeable'");
    }

    @Test
    void duplicatedMultiplicity() {
        var result = parse("""
            from Restaurant r
            create Rest {
                sells [changeable, *, volatile, 15] := r.sells
            }
            """);

        assertThat(result).hasIssues("Duplicated modifier 'multiplicity'");
    }

    @Test
    void modifierAndExplicitType() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name: EString [changeable, id, 1] = r.name
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void inapplicableModifiers() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [containment] = r.name
                sells [id] := r.sells
            }
            """);

        assertThat(result).hasIssues(
            "Modifier 'containment' is not applicable to attributes",
            "Modifier 'id' is not applicable to references"
        );
    }

    @Test
    void multiplicityAssignSingleToMulti() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [*] = r.name
            }
            """);

        assertThat(result).hasIssues("Cannot assign a single value to a multi-valued feature");
    }

    @Test
    void multiplicityAssignMultiToSingle() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [1] := #[r.name]
            }
            """);

        assertThat(result).hasIssues("Cannot assign multiple values to a single-valued feature");
    }

    @Test
    void correctMultiplicity() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [*] := #[r.name]
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void multiplicityUpperBoundZero() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [1..0] := #[r.name]
            }
            """);

        assertThat(result).hasIssues("Upper bound must be at least 1");
    }

    @Test
    void multiplicityBoundsReversed() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [2..1] := #[r.name]
            }
            """);

        assertThat(result).hasIssues("Lower bound must be less than upper bound");
    }

    @Test
    void multiplicityBoundsEqual() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [1..1] = r.name
            }
            """);

        assertThat(result).hasIssues("Lower and upper bound are equal, consider using a single value instead");
    }

    @Test
    void multiplicityAtLeastZero() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [0..*] := #[r.name]
            }
            """);

        assertThat(result).hasIssues("Lower bound is 0, consider using '*' instead");
    }

    @Test
    void multiplicityAtLeastOne() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [1..*] := #[r.name]
            }
            """);

        assertThat(result).hasIssues("Lower bound is 1, consider using '+' instead");
    }

    @Test
    void multiplicityExactZero() {
        var result = parse("""
            from Restaurant r
            create Rest {
                name [0] := #[r.name]
            }
            """);

        assertThat(result).hasIssues("Exact multiplicity must be at least 1");
    }

}
