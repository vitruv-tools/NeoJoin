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
        Assertions.assertThat(results.size() == 1)
            .as("Expected a target class with name %s, but found %d", name, results.size()).isTrue();
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

}
