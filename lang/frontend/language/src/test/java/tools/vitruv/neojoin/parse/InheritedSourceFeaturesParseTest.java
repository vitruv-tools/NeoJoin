package tools.vitruv.neojoin.parse;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

public class InheritedSourceFeaturesParseTest extends AbstractInheritanceParseTest {

    /**
     * A query without body copies the inherited features of the source class (here {@code Animal.name}), so it
     * overrides the inherited feature {@code name} of the super class.
     */
    @Test
    void queryWithoutBodyOverridesFeatureWithInheritedSourceFeature() {
        var result = parseWithKennel("""
            abstract Named {
                name: EString [1]
            }

            from Dog create Hound extends Named
            """);

        assertThat(result).hasNoIssues();
    }

}
