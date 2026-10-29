package tools.vitruv.neojoin.parse;

import org.eclipse.xtext.diagnostics.Severity;
import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class OverridingFeatureNullabilityParseTest extends AbstractInheritanceParseTest {

    @Test
    void nullableExpressionForRequiredInheritedFeatureWarns() {
        var result = parse("""
            abstract B {
                n: EString [1]
            }

            from Restaurant r create C extends B {
                n := r.name?.toUpperCase
            }
            """);

        assertThat(result).hasIssueWithSeverity(
            Severity.WARNING, "Nullable expression used to initialize non-nullable feature"
        );
    }

    @Test
    void nullableExpressionForOptionalInheritedFeatureDoesNotWarn() {
        var result = parse("""
            abstract B {
                n: EString [?]
            }

            from Restaurant r create C extends B {
                n := r.name?.toUpperCase
            }
            """);

        assertThat(result).hasNoIssues();
    }

}
