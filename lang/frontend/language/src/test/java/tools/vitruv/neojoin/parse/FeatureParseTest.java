package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class FeatureParseTest extends AbstractParseTest {

    @Test
    void classAliasMissing() {
        var result = parse("""
            from Restaurant create Rest { r.name }
            """);

        assertThat(result).hasIssues("The method or field r is undefined");
    }

    @Test
    void unsupportedType() {
        var result = parse("""
            from Restaurant create {
                test := new java.util.concurrent.atomic.AtomicInteger()
            }
            """);

        assertThat(result).hasIssues("Unsupported type: AtomicInteger");
    }

    @Test
    void duplicatedFeatureName() {
        var result = parse("""
            from Restaurant r create {
                name := r.name
                name := r.name
            }
            """);

        assertThat(result).hasIssues("Duplicated feature name: name");
    }

    @Test
    void duplicatedInferredFeatureName() {
        var result = parse("""
            from Restaurant r create {
                r.name
                r.name
            }
            """);

        assertThat(result).hasIssues("Duplicated feature name: name");
    }

    @Test
    void duplicatedMixedFeatureName() {
        var result = parse("""
            from Restaurant r create {
                name := r.address
                r.name
            }
            """);

        assertThat(result).hasIssues("Duplicated feature name: name");
    }

    @Test
    void explicitCopyFeatureNoFeature() {
        var result = parse("""
            from Restaurant r create {
                test = r.address.length
            }
            """);

        assertThat(result).hasIssues("Copy feature expression does not reference a feature");
    }

    @Test
    void implicitCopyFeatureNoFeature() {
        var result = parse("""
            from Restaurant r create {
                r.address.length
            }
            """);

        assertThat(result).hasIssues("Copy feature expression does not reference a feature");
    }

    @Test
    void nullableExpressionForNonNullableCopiedFeature() {
        var result = parse("""
            from Restaurant r create {
                name = r?.name
            }
            """);

        assertThat(result).hasIssues("Nullable expression used to initialize non-nullable feature");
    }

    @Test
    void nullableExpressionForNullableCopiedFeature() {
        var result = parse("""
            from Restaurant r create {
                name [?] = r?.name
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void implicitTargetIsQueryWithJoin() {
        var result = parse("""
            from Restaurant r create {
                r.sells
            }

            from Food f1
            join Food f2
            create MixedFoods {}
            """);

        assertThat(result).hasIssues(
            "Inferred type 'MixedFoods' is a query with join which might be unintended and " +
                "can lead to errors during transformation. Use explicit type to clarify the intended type."
        );
    }

    @Test
    void implicitTargetIsQueryWithGroupBy() {
        var result = parse("""
            from Restaurant r create {
                r.sells
            }

            from Food f1
            group by f1.name
            create MixedFoods {}
            """);

        assertThat(result).hasIssues(
            "Inferred type 'MixedFoods' is a query with group by which might be unintended and " +
                "can lead to errors during transformation. Use explicit type to clarify the intended type."
        );
    }

    @Test
    void implicitClassAlias() {
        var result = parse("""
            from Restaurant create { it.name }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void implicitClassAliasImplicitAccess() {
        var result = parse("""
            from Restaurant create { name }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void noImplicitClassAlias() {
        var result = parse("""
            from Restaurant
            join Restaurant
            create { it.name }
            """);

        assertThat(result).hasIssues("The method or field it is undefined");
    }

    @Test
    void itAsFeature() {
        var result = parse("""
            from Restaurant create { it }
            """);

        assertThat(result).hasIssues("Copy feature expression does not reference a feature");
    }

}
