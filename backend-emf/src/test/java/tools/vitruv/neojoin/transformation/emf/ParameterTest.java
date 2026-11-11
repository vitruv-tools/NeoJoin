package tools.vitruv.neojoin.transformation.emf;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static tools.vitruv.neojoin.transformation.emf.InstanceModelAssertions.assertThat;

public class ParameterTest extends DefaultTransformationTest {

    @Test
    void attributeParameter() {
        var result = transform("""
            param minEmployees : EInt

            from Restaurant r where r.numEmployees >= minEmployees create { r.name }
            """, Map.of("minEmployees", 3));

        assertThat(result)
            .hasInstance("Restaurant", named("Pizzeria Toni"))
            .hasNoMoreInstances();
    }

    @Test
    void listParameter() {
        var result = transform("""
            param names : EList<EString>

            from Restaurant r where names.contains(r.name) create { r.name }
            """, Map.of("names", List.of("Brauhaus", "Unknown")));

        assertThat(result)
            .hasInstance("Restaurant", named("Brauhaus"))
            .hasNoMoreInstances();
    }

}
