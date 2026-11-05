package tools.vitruv.neojoin.parse.testutils;

import tools.vitruv.neojoin.ast.ConcreteBody;
import tools.vitruv.neojoin.ast.ConcreteFeature;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.utils.AstUtils;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Fail.fail;

public final class FeatureTestUtils {

    private FeatureTestUtils() {}

    public static ConcreteFeature getFeatureOrFail(Query query, String featureName) {
        var body = (ConcreteBody) requireNonNull(AstUtils.getBody(query));
        return requireNonNull(
            body.getFeatures()
                .stream().filter(feature -> feature.getName().equals(featureName))
                .findAny()
                .orElseGet(() -> fail("No feature with name: '%s' found in query.".formatted(featureName))));
    }
}
