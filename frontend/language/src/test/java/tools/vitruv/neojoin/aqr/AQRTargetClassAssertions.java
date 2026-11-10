package tools.vitruv.neojoin.aqr;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.vitruv.neojoin.model.AQRFeature;
import tools.vitruv.neojoin.model.AQRJoin;
import tools.vitruv.neojoin.model.AQRSource;
import tools.vitruv.neojoin.model.AQRTargetClass;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
        Assertions.assertThat(results.size())
            .as(
                "Expected a join with source class %s and alias %s, but found %d",
                source.getName(),
                alias,
                results.size()
            )
            .isSameAs(1);
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
        Assertions.assertThat(actual.source().conditions()).as(
                "Expected a condition in %s, but found none",
                actual.name()
            )
            .isNotEmpty();
        return this;
    }

    public AQRTargetClassAssertions hasConditions(int count) {
        hasSource();
        check(actual.source() != null);
        Assertions.assertThat(actual.source().conditions()).as(
                "Expected %d conditions in %s",
                count,
                actual.name()
            )
            .hasSize(count);
        return this;
    }

    public AQRTargetClassAssertions hasNoCondition() {
        isNotNull();
        if (actual.source() != null) {
            Assertions.assertThat(actual.source().conditions()).as(
                    "Expected no condition in '%s', but found some",
                    actual.name()
                )
                .isEmpty();
        }
        return this;
    }

    private AQRFeature getFeature(String name) {
        var results = actual.features().stream().filter(a -> a.name().equals(name)).toList();
        check(results.size() <= 1);
        Assertions.assertThat(results)
            .as("Expected a feature named '%s' in '%s', but found none", name, actual.name())
            .hasSize(1);
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
        } else if (feature instanceof AQRFeature.Reference || feature instanceof AQRFeature.EcoreReference) {
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
        if (kind instanceof AQRFeature.Kind.Copy copy) {
            failWithMessage(message + "copied from '%s'", copy.source());
        } else if (kind instanceof AQRFeature.Kind.Calculate) {
            failWithMessage(message + "calculated");
        } else if (kind instanceof AQRFeature.Kind.Generate) {
            failWithMessage(message + "generated");
        } else {
            fail();
        }
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

    @SuppressWarnings("DataFlowIssue")
    public AQRTargetClassAssertions hasCopiedEcoreReference(String name, EClass type, EStructuralFeature copyOf) {
        isNotNull();
        var feature = getFeature(name);
        if (feature instanceof AQRFeature.EcoreReference ref) {
            Assertions.assertThat(ref.type()).isSameAs(type);
            checkCopied(ref, copyOf);
        } else {
            failWithMessage("Expected feature named '%s' in '%s' to be an Ecore reference", name, actual.name());
        }
        return this;
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

    public AQRTargetClassAssertions isAbstract() {
        isNotNull();
        if (!actual.isAbstract()) {
            failWithMessage("Expected target class '%s' to be abstract", actual.name());
        }
        return this;
    }

    public AQRTargetClassAssertions isNotAbstract() {
        isNotNull();
        if (actual.isAbstract()) {
            failWithMessage("Expected target class '%s' to not be abstract", actual.name());
        }
        return this;
    }

    public AQRTargetClassAssertions hasNoSource() {
        isNotNull();
        Assertions.assertThat(actual.source())
            .as("Expected no source in %s, but found one", actual.name())
            .isNull();
        return this;
    }

    private AQRSource getSource() {
        hasSource();
        var source = actual.source();
        check(source != null);
        return source;
    }

    public AQRTargetClassAssertions sourceIncludesSubClasses() {
        if (!getSource().from().includeSubClasses()) {
            failWithMessage("Expected source of '%s' to include sub classes", actual.name());
        }
        return this;
    }

    public AQRTargetClassAssertions sourceExcludesSubClasses() {
        if (getSource().from().includeSubClasses()) {
            failWithMessage("Expected source of '%s' to not include sub classes", actual.name());
        }
        return this;
    }

    public AQRTargetClassAssertions joinIncludesSubClasses(EClassifier source, String alias, boolean expected) {
        var results = getSource().joins().stream()
            .filter(join -> join.from().clazz() == source && Objects.equals(join.from().alias(), alias))
            .toList();
        Assertions.assertThat(results).hasSize(1);
        Assertions.assertThat(results.get(0).from().includeSubClasses()).isEqualTo(expected);
        return this;
    }

    public AQRTargetClassAssertions hasNoSuperClasses() {
        isNotNull();
        Assertions.assertThat(actual.superClasses())
            .as("Expected no super classes in '%s'", actual.name())
            .isEmpty();
        return this;
    }

    /**
     * Checks the direct super classes (in order).
     */
    public AQRTargetClassAssertions hasSuperClasses(String... names) {
        isNotNull();
        Assertions.assertThat(actual.superClasses().stream().map(AQRTargetClass::name).toList())
            .as("Direct super classes of '%s'", actual.name())
            .containsExactly(names);
        return this;
    }

    /**
     * Checks the transitive super classes (in any order, without duplicates).
     */
    public AQRTargetClassAssertions hasAllSuperClasses(String... names) {
        isNotNull();
        Assertions.assertThat(actual.allSuperClasses().stream().map(AQRTargetClass::name).toList())
            .as("All super classes of '%s'", actual.name())
            .containsExactlyInAnyOrder(names);
        return this;
    }

    private AQRFeature.Kind.Override getOverride(AQRFeature feature) {
        if (feature.kind() instanceof AQRFeature.Kind.Override override) {
            Assertions.assertThat(override.overridden().name()).isEqualTo(feature.name());
            Assertions.assertThat(actual.superClasses().stream()
                    .flatMap(c -> c.features().stream())
                    .map(AQRFeature::name))
                .as(
                    "Expected '%s' to override a feature of a direct super class of '%s'",
                    feature.name(),
                    actual.name()
                )
                .contains(feature.name());
            // modifiers must be those of the overridden feature
            Assertions.assertThat(feature.options())
                .as("Modifiers of overriding feature '%s' in '%s'", feature.name(), actual.name())
                .isEqualTo(override.overridden().options());
            return override;
        } else {
            failWithMessage(
                "Expected feature named '%s' in '%s' to override a feature, but it was %s",
                feature.name(),
                actual.name(),
                feature.kind()
            );
            return fail();
        }
    }

    private void checkOverriding(
        AQRFeature feature,
        String expected,
        Predicate<AQRFeature.Kind> overridingKindCheck
    ) {
        var override = getOverride(feature);
        if (!overridingKindCheck.test(override.overriding())) {
            failWithMessage(
                "Expected feature '%s' in '%s' to override with %s, but overriding kind was %s",
                feature.name(),
                actual.name(),
                expected,
                override.overriding()
            );
        }
    }

    private void checkOverridingExplicitCopy(AQRFeature feature, EStructuralFeature source) {
        checkOverriding(feature, "an explicit copy", k -> k instanceof AQRFeature.Kind.Copy.Explicit);
        Assertions.assertThat(((AQRFeature.Kind.Override) feature.kind()).overriding().source()).isSameAs(source);
    }

    private void checkOverridingImplicitCopy(AQRFeature feature, EStructuralFeature source) {
        checkOverriding(feature, "an implicit copy", k -> k instanceof AQRFeature.Kind.Copy.Implicit);
        Assertions.assertThat(((AQRFeature.Kind.Override) feature.kind()).overriding().source()).isSameAs(source);
    }

    private void checkOverridingCalculated(AQRFeature feature) {
        checkOverriding(feature, "a calculation", k -> k instanceof AQRFeature.Kind.Calculate);
    }

    private void checkOverridingAbstract(AQRFeature feature) {
        checkOverriding(feature, "an abstract feature", k -> k instanceof AQRFeature.Kind.Abstract);
    }

    public AQRTargetClassAssertions hasAbstractAttribute(
        String name,
        String type,
        Consumer<AQRFeature.Options> optionsConsumer
    ) {
        var attr = getAttribute(name, type);
        if (!(attr.kind() instanceof AQRFeature.Kind.Abstract)) {
            failBecauseOf(attr.kind(), "abstract", attr);
        }
        optionsConsumer.accept(attr.options());
        return this;
    }

    public AQRTargetClassAssertions hasAbstractAttribute(String name, String type) {
        return hasAbstractAttribute(name, type, IgnoreOptions);
    }

    public AQRTargetClassAssertions hasAbstractReference(
        String name,
        String type,
        Consumer<AQRFeature.Options> optionsConsumer
    ) {
        var ref = getReference(name, type);
        if (!(ref.kind() instanceof AQRFeature.Kind.Abstract)) {
            failBecauseOf(ref.kind(), "abstract", ref);
        }
        optionsConsumer.accept(ref.options());
        return this;
    }

    public AQRTargetClassAssertions hasAbstractReference(String name, String type) {
        return hasAbstractReference(name, type, IgnoreOptions);
    }

    public AQRTargetClassAssertions hasOverridingCopiedAttribute(
        String name,
        EStructuralFeature copyOf,
        Consumer<AQRFeature.Options> optionsConsumer
    ) {
        var attr = getAttribute(name, copyOf.getEType().getName());
        checkOverridingExplicitCopy(attr, copyOf);
        optionsConsumer.accept(attr.options());
        return this;
    }

    public AQRTargetClassAssertions hasOverridingCopiedAttribute(String name, EStructuralFeature copyOf) {
        return hasOverridingCopiedAttribute(name, copyOf, IgnoreOptions);
    }

    public AQRTargetClassAssertions hasOverridingImplicitlyCopiedAttribute(
        String name,
        EStructuralFeature copyOf,
        Consumer<AQRFeature.Options> optionsConsumer
    ) {
        var attr = getAttribute(name, copyOf.getEType().getName());
        checkOverridingImplicitCopy(attr, copyOf);
        optionsConsumer.accept(attr.options());
        return this;
    }

    public AQRTargetClassAssertions hasOverridingImplicitlyCopiedAttribute(String name, EStructuralFeature copyOf) {
        return hasOverridingImplicitlyCopiedAttribute(name, copyOf, IgnoreOptions);
    }

    public AQRTargetClassAssertions hasOverridingCalculatedAttribute(
        String name,
        String type,
        Consumer<AQRFeature.Options> optionsConsumer
    ) {
        var attr = getAttribute(name, type);
        checkOverridingCalculated(attr);
        optionsConsumer.accept(attr.options());
        return this;
    }

    public AQRTargetClassAssertions hasOverridingCalculatedAttribute(String name, String type) {
        return hasOverridingCalculatedAttribute(name, type, IgnoreOptions);
    }

    public AQRTargetClassAssertions hasOverridingAbstractAttribute(
        String name,
        String type,
        Consumer<AQRFeature.Options> optionsConsumer
    ) {
        var attr = getAttribute(name, type);
        checkOverridingAbstract(attr);
        optionsConsumer.accept(attr.options());
        return this;
    }

    public AQRTargetClassAssertions hasOverridingAbstractAttribute(String name, String type) {
        return hasOverridingAbstractAttribute(name, type, IgnoreOptions);
    }

    public AQRTargetClassAssertions hasOverridingCopiedReference(
        String name,
        String type,
        EStructuralFeature copyOf,
        Consumer<AQRFeature.Options> optionsConsumer
    ) {
        var ref = getReference(name, type);
        checkOverridingExplicitCopy(ref, copyOf);
        optionsConsumer.accept(ref.options());
        return this;
    }

    public AQRTargetClassAssertions hasOverridingCopiedReference(
        String name,
        String type,
        EStructuralFeature copyOf
    ) {
        return hasOverridingCopiedReference(name, type, copyOf, IgnoreOptions);
    }

    @SuppressWarnings("DataFlowIssue")
    public AQRTargetClassAssertions hasOverridingCopiedEcoreReference(
        String name,
        EClass type,
        EStructuralFeature copyOf
    ) {
        isNotNull();
        var feature = getFeature(name);
        if (feature instanceof AQRFeature.EcoreReference ref) {
            Assertions.assertThat(ref.type()).isSameAs(type);
            checkOverridingExplicitCopy(ref, copyOf);
        } else {
            failWithMessage("Expected feature named '%s' in '%s' to be an Ecore reference", name, actual.name());
        }
        return this;
    }

    /**
     * Checks the names of all features (in any order).
     */
    public AQRTargetClassAssertions hasFeatureNames(String... names) {
        isNotNull();
        List<String> featureNames = actual.features().stream().map(AQRFeature::name).toList();
        Assertions.assertThat(featureNames).containsExactlyInAnyOrder(names);
        return this;
    }

    public AQRTargetClassAssertions hasNoMoreFeatures() {
        isNotNull();
        var remaining = actual.features().stream().filter(f -> !seenFeatures.contains(f)).toList();
        Assertions.assertThat(remaining).isEmpty();
        return this;
    }

}
