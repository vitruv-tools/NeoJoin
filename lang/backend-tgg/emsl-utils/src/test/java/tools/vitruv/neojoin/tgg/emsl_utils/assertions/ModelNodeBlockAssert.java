package tools.vitruv.neojoin.tgg.emsl_utils.assertions;

import org.assertj.core.api.AbstractAssert;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emoflon.neo.emsl.eMSL.ModelNodeBlock;
import org.emoflon.neo.emsl.eMSL.ModelPropertyStatement;
import org.emoflon.neo.emsl.eMSL.ModelRelationStatement;
import org.emoflon.neo.emsl.eMSL.Value;

import java.util.List;

public class ModelNodeBlockAssert extends AbstractAssert<ModelNodeBlockAssert, ModelNodeBlock> {

    protected ModelNodeBlockAssert(ModelNodeBlock actual) {
        super(actual, ModelNodeBlockAssert.class);
    }

    public static ModelNodeBlockAssert assertThat(ModelNodeBlock actual) {
        return new ModelNodeBlockAssert(actual);
    }

    public ModelNodeBlockAssert hasPropertyCount(int expectedCount) {
        isNotNull();
        if (actual.getProperties().size() != expectedCount) {
            failWithMessage(
                    "Expected <%s> properties but found <%s>",
                    expectedCount, actual.getProperties().size());
        }
        return this;
    }

    public ModelNodeBlockAssert hasEqualsProperty(String propName, Value expectedValue) {
        isNotNull();

        ModelPropertyStatement property =
                actual.getProperties().stream()
                        .filter(p -> p.getType().getName().equals(propName))
                        .findFirst()
                        .orElse(null);
        if (property == null) {
            failWithMessage("Expected to find property with name <%s>", propName);
        }

        if (!EcoreUtil.equals(property.getValue(), expectedValue)) {
            failWithMessage(
                    "Expected property <%s> to be <%s> but was <%s>",
                    propName, expectedValue, property.getValue());
        }

        return this;
    }

    public ModelNodeBlockAssert hasRelationTargets(
            String relationName, List<ModelNodeBlock> expectedTargets) {
        isNotNull();

        List<ModelNodeBlock> actualTargets =
                actual.getRelations().stream()
                        .filter(
                                r ->
                                        r.getTypes()
                                                .getFirst()
                                                .getType()
                                                .getName()
                                                .equals(relationName))
                        .map(ModelRelationStatement::getTarget)
                        .toList();
        if (!actualTargets.equals(expectedTargets)) {
            failWithMessage(
                    "Expected node block <%s> to have relation <%s> with exact targets:\n  <%s>\nbut found:\n  <%s>",
                    actual.getName(), relationName, expectedTargets, actualTargets);
        }

        return this;
    }

    public ModelNodeBlockAssert hasRelationCount(int expectedSize) {
        isNotNull();
        int actualSize = actual.getRelations().size();
        if (actualSize != expectedSize) {
            failWithMessage(
                    "Expected node block <%s> to have <%s> relations but found <%s>",
                    actual.getName(), expectedSize, actualSize);
        }
        return this;
    }

    public ModelNodeBlockAssert hasNoRelations() {
        isNotNull();
        if (!actual.getRelations().isEmpty()) {
            failWithMessage(
                    "Expected node block to have no relations but found <%s>",
                    actual.getRelations().size());
        }
        return this;
    }
}
