package tools.vitruv.neojoin.expression_parser.model;

import java.util.Objects;

public final class FeatureInformation {
    private final String featureName;
    private final String featureClassSimpleName;
    private final String featureClassIdentifier;

    public FeatureInformation(
            String featureName, String featureClassSimpleName, String featureClassIdentifier) {
        this.featureName = featureName;
        this.featureClassSimpleName = featureClassSimpleName;
        this.featureClassIdentifier = featureClassIdentifier;
    }

    public String getFeatureName() {
        return featureName;
    }

    public String getFeatureClassSimpleName() {
        return featureClassSimpleName;
    }

    public String getFeatureClassIdentifier() {
        return featureClassIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FeatureInformation that)) {
            return false;
        }
        return Objects.equals(featureName, that.featureName)
                && Objects.equals(featureClassSimpleName, that.featureClassSimpleName)
                && Objects.equals(featureClassIdentifier, that.featureClassIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(featureName, featureClassSimpleName, featureClassIdentifier);
    }

    @Override
    public String toString() {
        return "FeatureInformation(featureName="
                + featureName
                + ", featureClassSimpleName="
                + featureClassSimpleName
                + ", featureClassIdentifier="
                + featureClassIdentifier
                + ")";
    }
}
