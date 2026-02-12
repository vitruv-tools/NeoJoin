package tools.vitruv.neojoin.aqr;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static tools.vitruv.neojoin.utils.Assertions.check;
import static tools.vitruv.neojoin.utils.Assertions.fail;

public class AQRTargetClassAssertions extends AbstractAssert<AQRTargetClassAssertions, AQRTargetClass> {

    private final Set<AQRFeature> seenFeatures = new HashSet<>();
    private final Set<AQRJoin> seenJoins = new HashSet<>();

    protected AQRTargetClassAssertions(AQRTargetClass actual) {
        super(actual, AQRTargetClassAssertions.class);
    }

    public static AQRTargetClassAssertions assertThat(AQRTargetClass actual) {
        return new AQRTargetClassAssertions(actual);
    }

    public AQRTargetClassAssertions hasSource() {
        isNotNull();
        Assertions.assertThat(actual.source())
            .as("Expected a source in %s, but found none", actual.name())
            .isNotNull();
        return this;
    }

    public AQRTargetClassAssertions sourceIs(EClassifier source) {
        hasSource();
        check(actual.source() != null);
        Assertions.assertThat(actual.source().from().clazz()).isSameAs(source);
        return this;
    }

    public AQRTargetClassAssertions sourceIs(EClassifier source, String alias) {
        sourceIs(source);
        check(actual.source() != null);
        Assertions.assertThat(actual.source().from().alias()).isEqualTo(alias);
        return this;
    }

    public AQRTargetClassAssertions hasJoin(EClassifier source, String alias, AQRJoin.Type type) {
        hasSource();
        check(actual.source() != null);
        var results = actual.source().joins().stream()
            .filter(join -> join.from().clazz() == source && Objects.equals(join.from().alias(), alias))
            .toList();
        Assertions.assertThat(results.size() == 1).as(
            "Expected a join with source class %s and alias %s, but found %d",
            source.getName(),
            alias,
            results.size()
        ).isTrue();
        var join = results.get(0);
        Assertions.assertThat(join.type()).isSameAs(type);
        seenJoins.add(join);
        return this;
    }

    public AQRTargetClassAssertions hasNoMoreJoins() {
        isNotNull();
        if (actual.source() != null) {
            var remaining = actual.source().joins().stream().filter(f -> !seenJoins.contains(f)).toList();
            Assertions.assertThat(remaining).isEmpty();
        }
        return this;
    }

    public AQRTargetClassAssertions hasCondition() {
        hasSource();
        check(actual.source() != null);
        Assertions.assertThat(actual.source().condition()).as(
                "Expected a condition in %s, but found none",
                actual.name()
            )
            .isNotNull();
        return this;
    }

    public AQRTargetClassAssertions hasNoCondition() {
        isNotNull();
        if (actual.source() != null) {
            Assertions.assertThat(actual.source().condition()).as(
                    "Expected no condition in '%s', but found one",
                    actual.name()
                )
                .isNull();
        }
        return this;
    }

    private AQRFeature getFeature(String name) {
        var results = actual.features().stream().filter(a -> a.name().equals(name)).toList();
        check(results.size() <= 1);
        Assertions.assertThat(results.size() == 1)
            .as("Expected a feature named '%s' in '%s', but found none", name, actual.name())
            .isTrue();
        var feature = results.get(0);
        seenFeatures.add(feature);
        return feature;
    }

    @SuppressWarnings("DataFlowIssue")
    private AQRFeature.Attribute getAttribute(String name, String type) {
        isNotNull();
        var feature = getFeature(name);
        if (feature instanceof AQRFeature.Attribute attr) {
            Assertions.assertThat(attr.type().getName()).isEqualTo(type);
            return attr;
        } else {
            failWithMessage(
                "Expected feature named '%s' in '%s' to be an attribute, but it was a reference",
                name,
                actual.name()
            );
            return null;
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public AQRFeature.Reference getReference(String name, String type) {
        isNotNull();
        var feature = getFeature(name);
        if (feature instanceof AQRFeature.Reference ref) {
            Assertions.assertThat(ref.type().name()).isEqualTo(type);
            return ref;
        } else {
            failWithMessage(
                "Expected feature named '%s' in '%s' to be a reference, but it was an attribute",
                name,
                actual.name()
            );
            return null;
        }
    }

    private static String getFeatureTypeName(AQRFeature feature) {
        if (feature instanceof AQRFeature.Attribute) {
            return "attribute";
        } else if (feature instanceof AQRFeature.Reference) {
            return "reference";
        } else {
            return fail();
        }
    }

    private void failBecauseOf(AQRFeature.Kind kind, String expected, AQRFeature feature) {
        var type = getFeatureTypeName(feature);
        var message = "Expected %s named '%s' in '%s' to be %s, but it was ".formatted(
            type,
            feature.name(),
            actual.name(),
            expected
        );
        if (kind instanceof AQRFeature.Kind.Copy copy) { failWithMessage(message + "copied from '%s'", copy.source()); }
        else if (kind instanceof AQRFeature.Kind.Calculate) { failWithMessage(message + "calculated"); }
        else if (kind instanceof AQRFeature.Kind.Generate) { failWithMessage(message + "generated"); }
        else { fail(); }
    }

    private void checkCopied(AQRFeature feature, EStructuralFeature source) {
        if (feature.kind() instanceof AQRFeature.Kind.Copy copy) {
            Assertions.assertThat(copy.source()).isSameAs(source);
        } else {
            failBecauseOf(feature.kind(), "copied", feature);
        }
    }

    private void checkCalculated(AQRFeature feature) {
        if (!(feature.kind() instanceof AQRFeature.Kind.Calculate)) {
            failBecauseOf(feature.kind(), "calculated", feature);
        }
    }

    private void checkGenerated(AQRFeature feature) {
        if (!(feature.kind() instanceof AQRFeature.Kind.Generate)) {
            failBecauseOf(feature.kind(), "generated", feature);
        }
    }

    private static final Consumer<AQRFeature.Options> IgnoreOptions = options -> {};

    public AQRTargetClassAssertions hasCopiedAttribute(
        String name,
        EStructuralFeature copyOf,
        Consumer<AQRFeature.Options> optionsConsumer
    ) {
        var attr = getAttribute(name, copyOf.getEType().getName());
        checkCopied(attr, copyOf);
        optionsConsumer.accept(attr.options());
        return this;
    }

    public AQRTargetClassAssertions hasCopiedAttribute(String name, EStructuralFeature copyOf) {
        return hasCopiedAttribute(name, copyOf, IgnoreOptions);
    }

    public AQRTargetClassAssertions hasCalculatedAttribute(
        String name,
        String type,
        Consumer<AQRFeature.Options> optionsConsumer
    ) {
        var attr = getAttribute(name, type);
        checkCalculated(attr);
        optionsConsumer.accept(attr.options());
        return this;
    }

    public AQRTargetClassAssertions hasCalculatedAttribute(String name, String type) {
        return hasCalculatedAttribute(name, type, IgnoreOptions);
    }

    public AQRTargetClassAssertions hasCopiedReference(
        String name,
        String type,
        EStructuralFeature copyOf,
        Consumer<AQRFeature.Options> optionsConsumer
    ) {
        var ref = getReference(name, type);
        checkCopied(ref, copyOf);
        optionsConsumer.accept(ref.options());
        return this;
    }

    public AQRTargetClassAssertions hasCopiedReference(String name, String type, EStructuralFeature copyOf) {
        return hasCopiedReference(name, type, copyOf, IgnoreOptions);
    }

    public AQRTargetClassAssertions hasCalculatedReference(
        String name,
        String type,
        Consumer<AQRFeature.Options> optionsConsumer
    ) {
        var ref = getReference(name, type);
        checkCalculated(ref);
        optionsConsumer.accept(ref.options());
        return this;
    }

    public AQRTargetClassAssertions hasCalculatedReference(String name, String type) {
        return hasCalculatedReference(name, type, IgnoreOptions);
    }

    public AQRTargetClassAssertions hasGeneratedReference(
        String name,
        String type,
        Consumer<AQRFeature.Options> optionsConsumer
    ) {
        var ref = getReference(name, type);
        checkGenerated(ref);
        optionsConsumer.accept(ref.options());
        return this;
    }

    public AQRTargetClassAssertions hasGeneratedReference(String name, String type) {
        return hasGeneratedReference(name, type, IgnoreOptions);
    }

    public AQRTargetClassAssertions hasNoMoreFeatures() {
        isNotNull();
        var remaining = actual.features().stream().filter(f -> !seenFeatures.contains(f)).toList();
        Assertions.assertThat(remaining).isEmpty();
        return this;
    }

}
