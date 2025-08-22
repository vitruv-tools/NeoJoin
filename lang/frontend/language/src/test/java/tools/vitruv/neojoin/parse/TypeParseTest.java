package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class TypeParseTest extends AbstractParseTest {

    @Test
    void correctTypeAttribute() {
        var result = parse("""
            from Restaurant r create {
                name: EString := r.name
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void wrongDataTypeAttribute() {
        var result = parse("""
            from Restaurant r create {
                name: EInt := r.name
            }
            """);

        assertThat(result).hasIssues("Type mismatch: cannot convert from String to int");
    }

    @Test
    void classTypeAttribute() {
        var result = parse("""
            from Restaurant r create {
                name: TastyFood := r.name
            }

            from Food create TastyFood
            """);

        assertThat(result).hasIssues("Type mismatch: cannot convert from EString to TastyFood");
    }

    @Test
    void correctTypeMultiReference() {
        var result = parse("""
            from Restaurant r create {
                sells: TastyFood := r.sells
            }

            from Food create TastyFood
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void correctTypeSingleReference() {
        var result = parse("""
            from Restaurant r create {
                sells: TastyFood := r.sells.first
            }

            from Food create TastyFood
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void dataTypeReference() {
        var result = parse("""
            from Restaurant r create {
                sells: EString := r.sells.first
            }

            from Food create TastyFood
            """);

        assertThat(result).hasIssues("Type mismatch: cannot convert from Food to String");
    }

    @Test
    void wrongClassReference() {
        var result = parse("""
            from Restaurant r create {
                sells: Restaurant := r.sells.first
            }

            from Food create TastyFood
            """);

        assertThat(result).hasIssues("Type mismatch: cannot convert from Food to Restaurant");
    }

    @Test
    void nullFeatureWithType() {
        var result = parse("""
            from Restaurant r create {
                test: Food := null
            }

            from Food create
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void nullFeatureWithoutType() {
        var result = parse("""
            from Restaurant r create {
                test := null
            }

            from Food create
            """);

        assertThat(result).hasIssues("Cannot infer type");
    }

    @Test
    void ambiguousImplicitTargetClass() {
        var result = parse("""
            from Restaurant r create {
                r.sells
            }

            from Food create TastyFood
            from Food create NotSoTastyFood
            """);

        assertThat(result)
            .hasIssues("Ambiguous target class for source class 'Food'. Possible candidates: NotSoTastyFood, TastyFood");
    }

    @Test
    void ambiguousTargetClassWithinCopiedClass() {
        var result = parse("""
            from Restaurant create

            from Food create TastyFood
            from Food create NotSoTastyFood
            """);

        assertThat(result)
            .hasIssues("Ambiguous target class for source class 'Food' while copying reference 'Restaurant::sells'. Possible candidates: NotSoTastyFood, TastyFood");
    }

    @Test
    void ambiguousTargetClassWithinRecursivelyCopiedClass() {
        var result = parse("""
            from rest.Store create

            from Food create TastyFood
            from Food create NotSoTastyFood
            """);

        assertThat(result)
            .hasIssues(
                "Ambiguous target class for source class 'Food' while copying reference 'Store::foods'. Possible candidates: NotSoTastyFood, TastyFood",
                "Ambiguous target class for source class 'Food' while copying reference 'Restaurant::sells'. Possible candidates: NotSoTastyFood, TastyFood"
            );
    }

    @Test
    void nonAmbiguousTargetClassWithinRecursivelyAndCyclicCopiedClass() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://vitruv.tools/cyclic"

            from Root create Base
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void ambiguousTargetClassWithinRecursivelyAndCyclicCopiedClass() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://vitruv.tools/cyclic"

            from Root create Base

            from Child create Child1
            from Child create Child2
            """);

        assertThat(result)
            .hasIssues("Ambiguous target class for source class 'Child' while copying reference 'Parent::child'. Possible candidates: Child1, Child2");
    }

    @Test
    void unresolvedExplicitTargetClass() {
        var result = parse("""
            from Restaurant r create {
                sells: Food = r.sells
            }

            from Food create TastyFood
            """);

        assertThat(result)
            .hasIssues("Food cannot be resolved.");
    }

}
