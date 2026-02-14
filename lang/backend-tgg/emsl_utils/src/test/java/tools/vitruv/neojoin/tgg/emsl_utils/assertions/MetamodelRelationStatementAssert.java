package tools.vitruv.neojoin.tgg.emsl_utils.assertions;

import org.assertj.core.api.AbstractAssert;
import org.emoflon.neo.emsl.eMSL.MetamodelNodeBlock;
import org.emoflon.neo.emsl.eMSL.MetamodelRelationStatement;
import org.emoflon.neo.emsl.eMSL.RelationKind;

import java.util.Objects;

public class MetamodelRelationStatementAssert
        extends AbstractAssert<MetamodelRelationStatementAssert, MetamodelRelationStatement> {

    protected MetamodelRelationStatementAssert(MetamodelRelationStatement actual) {
        super(actual, MetamodelRelationStatementAssert.class);
    }

    public static MetamodelRelationStatementAssert assertThat(MetamodelRelationStatement actual) {
        return new MetamodelRelationStatementAssert(actual);
    }

    public MetamodelRelationStatementAssert hasKind(RelationKind expectedKind) {
        isNotNull();
        if (actual.getKind() != expectedKind) {
            failWithMessage(
                    "Expected relation <%s> to have kind <%s> but was <%s>",
                    actual.getName(), expectedKind, actual.getKind());
        }
        return this;
    }

    public MetamodelRelationStatementAssert hasName(String expectedName) {
        isNotNull();
        if (!Objects.equals(actual.getName(), expectedName)) {
            failWithMessage(
                    "Expected relation name to be <%s> but was <%s>",
                    expectedName, actual.getName());
        }
        return this;
    }

    public MetamodelRelationStatementAssert hasTarget(MetamodelNodeBlock expectedTarget) {
        isNotNull();

        if (actual.getTarget() == null) {
            failWithMessage(
                    "Expected relation <%s> to have a target, but target was null",
                    actual.getName());
        }

        if (!Objects.equals(actual.getTarget(), expectedTarget)) {
            failWithMessage(
                    "Expected relation <%s> to have target <%s> but was <%s>",
                    actual, expectedTarget, actual.getTarget());
        }

        return this;
    }

    public MetamodelRelationStatementAssert hasLowerBound(String expectedLower) {
        isNotNull();
        if (!Objects.equals(actual.getLower(), expectedLower)) {
            failWithMessage(
                    "Expected relation <%s> to have lower bound <%s> but was <%s>",
                    actual.getName(), expectedLower, actual.getLower());
        }
        return this;
    }

    public MetamodelRelationStatementAssert hasUpperBound(String expectedUpper) {
        isNotNull();
        if (!Objects.equals(actual.getUpper(), expectedUpper)) {
            failWithMessage(
                    "Expected relation <%s> to have upper bound <%s> but was <%s>",
                    actual.getName(), expectedUpper, actual.getUpper());
        }
        return this;
    }
}
