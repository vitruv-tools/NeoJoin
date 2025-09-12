package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class TypeParseTest extends AbstractParseTest {

    @Test
    void correctTypeAttribute() {
        var result = parse("""
            from Restaurant r create {
                name: EString := r.name
                idPrimitive: EInt := 1
                idObject: EInt := 1 as Integer
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

        assertThat(result).hasIssues("Type mismatch: cannot convert from EString (String) to EInt (int)");
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

        assertThat(result).hasIssues("Type mismatch: cannot convert from Food to EString");
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

    @Test
    void typeCastViaExplicitType() {
        var result = internalParse("""
            export package to "http://example.com"
            import "http://vitruv.tools/typecasts"

            from Test create {
                p1: EByte = it.attrShort
                p2: EShort = it.attrShort
                p3: EInt = it.attrShort
                p4: ELong = it.attrShort
                p5: EFloat = it.attrShort
                p6: EDouble = it.attrShort
                p7: EChar = it.attrShort
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void typeCastViaExplicitTypeFromBoxed() {
        var result = internalParse("""
            export package to "http://example.com"
            import "http://vitruv.tools/typecasts"

            from Test create {
                p1: EByte = it.attrShortObj
                p2: EShort = it.attrShortObj
                p3: EInt = it.attrShortObj
                p4: ELong = it.attrShortObj
                p5: EFloat = it.attrShortObj
                p6: EDouble = it.attrShortObj
                p7: EChar = it.attrShortObj
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void typeCastToBoxed() {
        var result = internalParse("""
            export package to "http://example.com"
            import "http://vitruv.tools/typecasts"

            from Test create {
                p1: EByteObject = it.attrShort
                p2: EShortObject = it.attrShort
                p3: EIntegerObject = it.attrShort
                p4: ELongObject = it.attrShort
                p5: EFloatObject = it.attrShort
                p6: EDoubleObject = it.attrShort
                p7: ECharacterObject = it.attrShort
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void invalidTypeCasts() {
        var result = internalParse("""
            export package to "http://example.com"
            import "http://vitruv.tools/typecasts"

            from Test create {
                p1: EBoolean = it.attrShort
                p2: EString = it.attrShort
                p3: EShort := true
                p4: EBoolean := it.attrShortObj
            }
            """);

        assertThat(result).hasIssues(
            "Type mismatch: cannot convert from EShort (short) to EBoolean (boolean)",
            "Type mismatch: cannot convert from EShort (short) to EString (String)",
            "Type mismatch: cannot convert from EBoolean (boolean) to EShort (short)",
            "Type mismatch: cannot convert from EShortObject (Short) to EBoolean (boolean)"
        );
    }

}
