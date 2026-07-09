package tools.vitruv.neojoin.parse;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.xtext.validation.Issue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.Pair;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

import static tools.vitruv.neojoin.parse.ParseAssertions.assertThat;
import static tools.vitruv.neojoin.parse.testutils.FeatureTestUtils.getFeatureOrFail;
import static tools.vitruv.neojoin.parse.testutils.TypeTestUtils.*;

@SuppressWarnings("HttpUrlsUsage")
public class InferredTypeTest extends AbstractParseTest {

    @Override
    protected Pair<ViewTypeDefinition, List<Issue>> parse(String query) {
        return internalParse("""
            export package to "http://example.com"

            import "http://example.org/imdb"

            """ + query);
    }

    private String filmSummaryPerYearQuery(String summeryBody) {
        return """
            from Film films
            group by films.year
            create Summary {
                %s
            }
            """.formatted(summeryBody);
    }

    private static Stream<CheckInferredFeatureTypeTuple> inferUnboxedType() {
        return Stream.of(
            new CheckInferredFeatureTypeTuple(
                "year",
                "year := films.same[ it.year ]",
                EcorePackage.Literals.EINT
            ),
            new CheckInferredFeatureTypeTuple(
                "n",
                "n := 42",
                EcorePackage.Literals.EINT
            ),
            new CheckInferredFeatureTypeTuple(
                "isItTheYear",
                "isItTheYear := true",
                EcorePackage.Literals.EBOOLEAN
            ),
            new CheckInferredFeatureTypeTuple(
                "it",
                "it := 'helo world'.charAt(0)",
                EcorePackage.Literals.ECHAR
            ),
            new CheckInferredFeatureTypeTuple(
                "it",
                "it := 'helo world'.getBytes().get(2)",
                EcorePackage.Literals.EBYTE
            )
        );
    }

    private static Stream<CheckInferredFeatureTypeTuple> inferListOfPrimitives() {
        return Stream.of(
            new CheckInferredFeatureTypeTuple(
                "years",
                "years := films.map[ it.year ]",
                EcorePackage.Literals.EINTEGER_OBJECT,
                true
            ),
            new CheckInferredFeatureTypeTuple(
                "years",
                "years := #[1, 2, 3]",
                EcorePackage.Literals.EINTEGER_OBJECT,
                true
            )
        );
    }

    private static Stream<CheckInferredFeatureTypeTuple> inferByteArray() {
        return Stream.of(
            new CheckInferredFeatureTypeTuple(
                "it",
                "it := 'foobar'.getBytes()",
                EcorePackage.Literals.EBYTE_ARRAY
            )
        );
    }

    @ParameterizedTest
    @MethodSource({"inferUnboxedType", "inferListOfPrimitives", "inferByteArray"})
    void runCheckInferredFeatureTypeTest(CheckInferredFeatureTypeTuple fixture) {
        var expressionHelper = getInjector().getInstance(ExpressionHelper.class);
        var result = parse(filmSummaryPerYearQuery(fixture.featureDeclaration));

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

    @Test
    void nullFeatureWithoutType() {
        var result = parse("""
            from Film f create {
                test := null
            }
            """);

        assertThat(result).hasIssues("Cannot infer type");
    }

    private record CheckInferredFeatureTypeTuple(
        String featureName,
        String featureDeclaration,
        EClassifier expectedClassifier,
        boolean shouldBeMany
    ) {

        CheckInferredFeatureTypeTuple(
            String featureName,
            String featureDeclaration,
            EClassifier expectedClassifier
        ) {
            this(featureName, featureDeclaration, expectedClassifier, false);
        }
    }
}
