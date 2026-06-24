package tools.vitruv.neojoin.parse.testutils;

import tools.vitruv.neojoin.ast.Feature;
import tools.vitruv.neojoin.ast.Query;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Fail.fail;

public class FeatureTestUtils {

    private FeatureTestUtils() {}

    public static Feature getFeatureOrFail(Query query, String featureName) {
        return requireNonNull(
            query.getBody().getFeatures()
                .stream().filter(feature -> feature.getName().equals(featureName))
                .findAny()
                .orElseGet(() -> fail("No feature with name: '%s' found in query.".formatted(featureName))));
    }
}
