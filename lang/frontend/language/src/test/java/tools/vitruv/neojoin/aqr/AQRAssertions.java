package tools.vitruv.neojoin.aqr;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AQRAssertions extends AbstractAssert<AQRAssertions, AQR> {

    private final Set<AQRTargetClass> seenTargetClasses = new HashSet<>();

    protected AQRAssertions(AQR actual) {
        super(actual, AQRAssertions.class);
    }

    public static AQRAssertions assertThat(AQR actual) {
        return new AQRAssertions(actual);
    }

    public AQRAssertions hasTargetClass(String name, Consumer<AQRTargetClass> consumer) {
        isNotNull();
        var results = actual.classes().stream()
            .filter(c -> c.name().equals(name))
            .toList();

        Assertions.assertThat(results.size())
                .as("Expected a target class with name %s, but found %d", name, results.size())
                .isSameAs(1);
        var target = results.get(0);
        consumer.accept(target);
        seenTargetClasses.add(target);
        return this;
    }

    public AQRAssertions hasRootTargetClass(String name, Consumer<AQRTargetClass> consumer) {
        return hasTargetClass(
            name, target -> {
                Assertions.assertThat(target)
                    .as("Expected target class %s to be root", name)
                    .isSameAs(actual.root());
                consumer.accept(target);
            }
        );
    }

    public void hasNoMoreTargetClasses() {
        hasNoMoreTargetClasses(false);
    }

    public void hasNoMoreTargetClasses(boolean includeRoot) {
        isNotNull();
        var remaining = actual.classes().stream()
            .filter(c -> !seenTargetClasses.contains(c))
            .filter(c -> includeRoot || c != actual.root())
            .toList();
        Assertions.assertThat(remaining)
            .as(
                "Expected no more target classes, but found: %s",
                remaining.stream().map(AQRTargetClass::name).collect(Collectors.joining())
            )
            .isEmpty();
    }

    public AQRAssertions hasParameter(String alias, String type, boolean isList) {
        isNotNull();
        var results = actual.parameters().stream()
            .filter(p -> p.alias().equals(alias))
            .toList();
        Assertions.assertThat(results.size() == 1)
            .as("Expected a parameter with alias %s, but found %d", alias, results.size()).isTrue();
        var parameter = results.get(0);
        Assertions.assertThat(parameter.type().getName()).as("Expected the parameter to have type %s, but it was of type %s", type, parameter.type().getName()).isEqualTo(type);
        Assertions.assertThat(parameter.isList()).as("Expected the parameter %s to be %s, but it is %s", parameter.alias(), isList ? "a list" : "a single value", parameter.isList() ? "a list" : "a single value").isEqualTo(isList);
        return this;
    }

}
