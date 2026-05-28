package tools.vitruv.neojoin.parse;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.xtext.xbase.XExpression;
import org.junit.jupiter.api.Test;
import tools.vitruv.neojoin.ast.Feature;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.jvmmodel.TypeInfo;
import tools.vitruv.neojoin.jvmmodel.TypeResolutionException;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;
import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;

@SuppressWarnings("HttpUrlsUsage")
class FeatureParseTest extends AbstractParseTest {
    private static final String FAILED_TO_RETRIEVE_TYPE_MESSAGE_PREFIX = "Failed to retrieve inferred type of feature:";

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

    @Test
    void inferUnboxedType() {
        var expressionHelper = getInjector().getInstance(ExpressionHelper.class);
        var fixtures = List.of(
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

        for (var fixture : fixtures) {
            runCheckInferredFeatureTypeTest(fixture);
        }
    }

    @Test
    void inferListOfPrimitives() {
        runCheckInferredFeatureTypeTest(
            new CheckInferredFeatureTypeFixture(
                "years",
                "years := films.map[ it.year ]",
                EcorePackage.Literals.EINTEGER_OBJECT,
                true
            )
        );
    }

    @Test
    void inferByteArray() {
        runCheckInferredFeatureTypeTest(
            new CheckInferredFeatureTypeFixture(
                "it",
                "it := 'foobar'.getBytes()",
                EcorePackage.Literals.EBYTE_ARRAY
            )
        );
    }

    private void runCheckInferredFeatureTypeTest(CheckInferredFeatureTypeFixture fixture) {
        assertThatNoException()
        .isThrownBy(super::setUpAIT);

        var expressionHelper = getInjector().getInstance(ExpressionHelper.class);
        var result = internalParse(
            filmSummeryPerYearQuery(fixture.featureDeclaration)
        );

        assertThat(result)
            .hasNoIssues();

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
