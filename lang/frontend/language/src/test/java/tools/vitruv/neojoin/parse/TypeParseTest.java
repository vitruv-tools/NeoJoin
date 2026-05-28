package tools.vitruv.neojoin.parse;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.xtext.xbase.XExpression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.vitruv.neojoin.ast.Feature;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.jvmmodel.TypeInfo;
import tools.vitruv.neojoin.jvmmodel.TypeResolutionException;

import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

@SuppressWarnings("HttpUrlsUsage")
class TypeParseTest extends AbstractParseTest {
    private static final String FAILED_TO_RETRIEVE_TYPE_MESSAGE_PREFIX = "Failed to retrieve inferred type of feature:";

    @Test
    void correctTypeAttribute() {
        var result = parse("""
            from Restaurant r create {
                name: EString := r.name
                idPrimitive: EInt := 1
                idObject: EInt := 1 as Integer
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void wrongDataTypeAttribute() {
        var result = parse("""
            from Restaurant r create {
                name: EInt := r.name
            }
            """);

        assertThat(result).hasIssues("Type mismatch: cannot convert from EString (String) to EInt (int)");
    }

    @Test
    void classTypeAttribute() {
        var result = parse("""
            from Restaurant r create {
                name: TastyFood := r.name
            }

            from Food create TastyFood
            """);

        assertThat(result).hasIssues("Type mismatch: cannot convert from EString to TastyFood");
    }

    @Test
    void correctTypeMultiReference() {
        var result = parse("""
            from Restaurant r create {
                sells: TastyFood := r.sells
            }

            from Food create TastyFood
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void correctTypeSingleReference() {
        var result = parse("""
            from Restaurant r create {
                sells: TastyFood := r.sells.first
            }

            from Food create TastyFood
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void dataTypeReference() {
        var result = parse("""
            from Restaurant r create {
                sells: EString := r.sells.first
            }

            from Food create TastyFood
            """);

        assertThat(result).hasIssues("Type mismatch: cannot convert from Food to EString");
    }

    @Test
    void wrongClassReference() {
        var result = parse("""
            from Restaurant r create {
                sells: Restaurant := r.sells.first
            }

            from Food create TastyFood
            """);

        assertThat(result).hasIssues("Type mismatch: cannot convert from Food to Restaurant");
    }

    @Test
    void nullFeatureWithType() {
        var result = parse("""
            from Restaurant r create {
                test: Food := null
            }

            from Food create
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void nullFeatureWithoutType() {
        var result = parse("""
            from Restaurant r create {
                test := null
            }

            from Food create
            """);

        assertThat(result).hasIssues("Cannot infer type");
    }

    @Test
    void ambiguousImplicitTargetClass() {
        var result = parse("""
            from Restaurant r create {
                r.sells
            }

            from Food create TastyFood
            from Food create NotSoTastyFood
            """);

        assertThat(result)
            .hasIssues("Ambiguous target class for source class 'Food'. Possible candidates: NotSoTastyFood, TastyFood");
    }

    @Test
    void ambiguousTargetClassWithinCopiedClass() {
        var result = parse("""
            from Restaurant create

            from Food create TastyFood
            from Food create NotSoTastyFood
            """);

        assertThat(result)
            .hasIssues("Ambiguous target class for source class 'Food' while copying reference 'Restaurant::sells'. Possible candidates: NotSoTastyFood, TastyFood");
    }

    @Test
    void ambiguousTargetClassWithinRecursivelyCopiedClass() {
        var result = parse("""
            from rest.Store create

            from Food create TastyFood
            from Food create NotSoTastyFood
            """);

        assertThat(result)
            .hasIssues(
                "Ambiguous target class for source class 'Food' while copying reference 'Store::foods'. Possible candidates: NotSoTastyFood, TastyFood",
                "Ambiguous target class for source class 'Food' while copying reference 'Restaurant::sells'. Possible candidates: NotSoTastyFood, TastyFood"
            );
    }

    @Test
    void nonAmbiguousTargetClassWithinRecursivelyAndCyclicCopiedClass() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://vitruv.tools/cyclic"

            from Root create Base
            """);

        assertThat(result)
            .hasNoIssues();
    }

    @Test
    void ambiguousTargetClassWithinRecursivelyAndCyclicCopiedClass() {
        var result = internalParse("""
            export package to "http://example.com"

            import "http://vitruv.tools/cyclic"

            from Root create Base

            from Child create Child1
            from Child create Child2
            """);

        assertThat(result)
            .hasIssues("Ambiguous target class for source class 'Child' while copying reference 'Parent::child'. Possible candidates: Child1, Child2");
    }

    @Test
    void unresolvedExplicitTargetClass() {
        var result = parse("""
            from Restaurant r create {
                sells: Food = r.sells
            }

            from Food create TastyFood
            """);

        assertThat(result)
            .hasIssues("Food cannot be resolved.");
    }

    @Test
    void typeCastViaExplicitType() {
        var result = internalParse("""
            export package to "http://example.com"
            import "http://vitruv.tools/typecasts"

            from Test create {
                p1: EByte = it.attrShort
                p2: EShort = it.attrShort
                p3: EInt = it.attrShort
                p4: ELong = it.attrShort
                p5: EFloat = it.attrShort
                p6: EDouble = it.attrShort
                p7: EChar = it.attrShort
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void typeCastViaExplicitTypeFromBoxed() {
        var result = internalParse("""
            export package to "http://example.com"
            import "http://vitruv.tools/typecasts"

            from Test create {
                p1: EByte = it.attrShortObj
                p2: EShort = it.attrShortObj
                p3: EInt = it.attrShortObj
                p4: ELong = it.attrShortObj
                p5: EFloat = it.attrShortObj
                p6: EDouble = it.attrShortObj
                p7: EChar = it.attrShortObj
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void typeCastToBoxed() {
        var result = internalParse("""
            export package to "http://example.com"
            import "http://vitruv.tools/typecasts"

            from Test create {
                p1: EByteObject = it.attrShort
                p2: EShortObject = it.attrShort
                p3: EIntegerObject = it.attrShort
                p4: ELongObject = it.attrShort
                p5: EFloatObject = it.attrShort
                p6: EDoubleObject = it.attrShort
                p7: ECharacterObject = it.attrShort
            }
            """);

        assertThat(result).hasNoIssues();
    }

    @Test
    void invalidTypeCasts() {
        var result = internalParse("""
            export package to "http://example.com"
            import "http://vitruv.tools/typecasts"

            from Test create {
                p1: EBoolean = it.attrShort
                p2: EString = it.attrShort
                p3: EShort := true
                p4: EBoolean := it.attrShortObj
            }
            """);

        assertThat(result).hasIssues(
            "Type mismatch: cannot convert from EShort (short) to EBoolean (boolean)",
            "Type mismatch: cannot convert from EShort (short) to EString (String)",
            "Type mismatch: cannot convert from EBoolean (boolean) to EShort (short)"
        );
    }

    private static Stream<CheckInferredFeatureTypeFixture> inferUnboxedType() {
        return Stream.of(
            new CheckInferredFeatureTypeFixture(
                "year",
                "year := films.same[ it.year ]",
                EcorePackage.Literals.EINT
            ),
            new CheckInferredFeatureTypeFixture(
                "n",
                "n := 42",
                EcorePackage.Literals.EINT
            ),
            new CheckInferredFeatureTypeFixture(
                "isItTheYear",
                "isItTheYear := true",
                EcorePackage.Literals.EBOOLEAN
            ),
            new CheckInferredFeatureTypeFixture(
                "it",
                "it := 'helo world'.charAt(0)",
                EcorePackage.Literals.ECHAR
            ),
            new CheckInferredFeatureTypeFixture(
                "it",
                "it := 'helo world'.getBytes().get(2)",
                EcorePackage.Literals.EBYTE
            )
        );
    }

    private static Stream<CheckInferredFeatureTypeFixture> inferListOfPrimitives() {
        return Stream.of(
            new CheckInferredFeatureTypeFixture(
                "years",
                "years := films.map[ it.year ]",
                EcorePackage.Literals.EINTEGER_OBJECT,
                true
            )
        );
    }

    private static Stream<CheckInferredFeatureTypeFixture> inferByteArray() {
        return Stream.of(
            new CheckInferredFeatureTypeFixture(
                "it",
                "it := 'foobar'.getBytes()",
                EcorePackage.Literals.EBYTE_ARRAY
            )
        );
    }

    @ParameterizedTest
    @MethodSource({"inferUnboxedType", "inferListOfPrimitives", "inferByteArray"})
    void runCheckInferredFeatureTypeTest(CheckInferredFeatureTypeFixture fixture) {
        var expressionHelper = getInjector().getInstance(ExpressionHelper.class);
        var result = internalParse(
            filmSummeryPerYearQuery(fixture.featureDeclaration)
        );

        assertThat(result).hasNoIssues();
        assertThat(result.left()).isNotNull();

        var query = result.left().getQueries().get(0);
        var feature = getFeatureOrFail(query, fixture.featureName);
        var featureType = inferredTypeOrFail(expressionHelper, feature.getExpression());
        var featureClassifier = getClassifierOrFail(featureType);

        assertThat(feature.getType())
            .as("Type should not have ben assigned.")
            .isNull();

        assertThat(featureType.isMany())
            .isEqualTo(fixture.shouldBeMany);

        assertThat(featureClassifier)
            .isEqualTo(fixture.expectedClassifier);
    }

    private String filmSummeryPerYearQuery(String summeryBody) {
        return """
            export package to "http://example.com"

            import "http://example.org/imdb"

            from Film films
            group by films.year
            create Summary {
                %s
            }
            """.formatted(summeryBody);
    }

    private record CheckInferredFeatureTypeFixture(
        String featureName,
        String featureDeclaration,
        EClassifier expectedClassifier,
        Boolean shouldBeMany
    ) {

        CheckInferredFeatureTypeFixture(
            String featureName,
            String featureDeclaration,
            EClassifier expectedClassifier
        ) {
            this(featureName, featureDeclaration, expectedClassifier, false);
        }
    }

    private Feature getFeatureOrFail(Query query, String featureName) {
        return requireNonNull(
            query.getBody().getFeatures()
                .stream().filter(feature -> feature.getName().equals(featureName))
                .findAny()
                .orElseGet(() -> fail("No feature with name: '%s' found in query.".formatted(featureName))));
    }

    private EClassifier getClassifierOrFail(TypeInfo typeInfo) {
        EClassifier classifier = typeInfo.classifier();

        return classifier == null ? fail("%s Classifier was null.".formatted(FAILED_TO_RETRIEVE_TYPE_MESSAGE_PREFIX))
            : classifier;
    }

    private TypeInfo inferredTypeOrFail(ExpressionHelper expressionHelper, XExpression expression) {
        TypeInfo typeInfo;
        try {
            typeInfo = expressionHelper.inferEType(expression);
        } catch (TypeResolutionException e) {
            typeInfo = fail(FAILED_TO_RETRIEVE_TYPE_MESSAGE_PREFIX, e);
        }

        return typeInfo == null ? fail("%s Type info was null.".formatted(FAILED_TO_RETRIEVE_TYPE_MESSAGE_PREFIX))
            : typeInfo;
    }
}
