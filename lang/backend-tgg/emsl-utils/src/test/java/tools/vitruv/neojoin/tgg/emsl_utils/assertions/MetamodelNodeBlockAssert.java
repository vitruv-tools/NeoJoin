package tools.vitruv.neojoin.tgg.emsl_utils.assertions;

import org.assertj.core.api.AbstractAssert;
import org.emoflon.neo.emsl.eMSL.BuiltInDataTypes;
import org.emoflon.neo.emsl.eMSL.MetamodelNodeBlock;
import org.emoflon.neo.emsl.eMSL.MetamodelPropertyStatement;
import org.emoflon.neo.emsl.eMSL.MetamodelRelationStatement;

import java.util.Objects;

public class MetamodelNodeBlockAssert
        extends AbstractAssert<MetamodelNodeBlockAssert, MetamodelNodeBlock> {

    protected MetamodelNodeBlockAssert(MetamodelNodeBlock actual) {
        super(actual, MetamodelNodeBlockAssert.class);
    }

    public static MetamodelNodeBlockAssert assertThat(MetamodelNodeBlock actual) {
        return new MetamodelNodeBlockAssert(actual);
    }

    public MetamodelNodeBlockAssert hasName(String expectedName) {
        isNotNull();
        if (!Objects.equals(actual.getName(), expectedName)) {
            failWithMessage(
                    "Expected node block name to be <%s> but was <%s>",
                    expectedName, actual.getName());
        }
        return this;
    }

    public MetamodelNodeBlockAssert hasPropertyCount(int expectedCount) {
        isNotNull();
        if (actual.getProperties().size() != expectedCount) {
            failWithMessage(
                    "Expected <%s> properties but found <%s>",
                    expectedCount, actual.getProperties().size());
        }
        return this;
    }

    public MetamodelNodeBlockAssert hasProperty(String propName, BuiltInDataTypes expectedType) {
        isNotNull();

        MetamodelPropertyStatement property =
                actual.getProperties().stream()
                        .filter(p -> p.getName().equals(propName))
                        .findFirst()
                        .orElse(null);
        if (property == null) {
            failWithMessage("Expected to find property with name <%s>", propName);
        }

        DataTypeAssert.assertThat(property.getType()).hasBuiltInReferenceType(expectedType);

        return this;
    }

    public MetamodelRelationStatementAssert relation(String relationName) {
        isNotNull();

        MetamodelRelationStatement relation =
                actual.getRelations().stream()
                        .filter(r -> r.getName().equals(relationName))
                        .findFirst()
                        .orElse(null);

        if (relation == null) {
            failWithMessage(
                    "Expected node block <%s> to have a relation named <%s>, but it was not found.",
                    actual.getName(), relationName);
        }

        return new MetamodelRelationStatementAssert(relation);
    }

    public MetamodelNodeBlockAssert hasRelationCount(int expectedSize) {
        isNotNull();
        int actualSize = actual.getRelations().size();
        if (actualSize != expectedSize) {
            failWithMessage(
                    "Expected node block <%s> to have <%s> relations but found <%s>",
                    actual.getName(), expectedSize, actualSize);
        }
        return this;
    }

    public MetamodelNodeBlockAssert hasNoRelations() {
        isNotNull();
        if (!actual.getRelations().isEmpty()) {
            failWithMessage(
                    "Expected node block to have no relations but found <%s>",
                    actual.getRelations().size());
        }
        return this;
    }
}
