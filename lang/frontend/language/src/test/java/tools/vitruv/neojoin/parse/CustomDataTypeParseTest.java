package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class CustomDataTypeParseTest extends AbstractParseTest {

    @Test
    void implicitDataType() {
        var result = parse("""
            from Restaurant r create {
                r.dailyRevenue
                dailyRevenue2 := r.dailyRevenue
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void implicitEnum() {
        var result = parse("""
            from Food f create {
                f.type
                type2 := f.type
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void explicitImportedEnum() {
        var result = parse("""
            from Food f create {
                type1: FoodType = f.type
                type2: rest.FoodType = f.type
                type3: FoodType := f.type
                type4: rest.FoodType := f.type
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void explicitImportedDataType() {
        var result = parse("""
            from Restaurant r create {
                dailyRevenue1: Money = r.dailyRevenue
                dailyRevenue2: Money := r.dailyRevenue
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void incompatibleDataType() {
        var result = parse("""
            from Food f create {
                test: Money = f.name
            }
            """);

        assertThat(result).hasIssues("Type mismatch: cannot convert from EString (String) to Money (float)");
    }

    @Test
    void incompatibleOtherToEnumType() {
        var result = parse("""
            from Food f create {
                test: FoodType = f.name
            }
            """);

        assertThat(result).hasIssues("Type mismatch: cannot convert from EString to FoodType");
    }

    @Test
    void incompatibleEnumTypeToOther() {
        var result = parse("""
            from Food f create {
                test: EString = f.type
            }
            """);

        assertThat(result).hasIssues("Type mismatch: cannot convert from FoodType to EString");
    }

}
