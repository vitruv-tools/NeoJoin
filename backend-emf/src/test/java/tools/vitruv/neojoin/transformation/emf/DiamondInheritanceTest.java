package tools.vitruv.neojoin.transformation.emf;

import org.junit.jupiter.api.Test;

import static tools.vitruv.neojoin.transformation.emf.InstanceModelAssertions.assertThat;

/**
 * Instance-level behavior of diamond inheritance.
 */
public class DiamondInheritanceTest extends AbstractKennelTest {

    /**
     * D extends B and C, which both extend the abstract class A. All declare the same attribute, D overrides it.
     * The overriding expression determines the value.
     */
    @Test
    void overridingFeatureOfDiamondIsEvaluated() {
        var result = transform("""
            abstract A {
                name: EString
            }

            abstract B extends A {
                name: EString
            }

            abstract C extends A {
                name: EString
            }

            from extends Dog d
            create D extends B, C {
                name := "dog:" + d.name
            }
            """);

        assertThat(result)
            .hasInstance("D", named("dog:Rex"))
            .hasInstance("D", named("dog:Bello"))
            .hasNoMoreInstances();
    }

}
