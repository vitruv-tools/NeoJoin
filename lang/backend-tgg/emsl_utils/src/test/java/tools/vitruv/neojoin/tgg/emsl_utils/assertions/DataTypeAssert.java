package tools.vitruv.neojoin.tgg.emsl_utils.assertions;

import org.assertj.core.api.AbstractAssert;
import org.emoflon.neo.emsl.eMSL.BuiltInDataTypes;
import org.emoflon.neo.emsl.eMSL.BuiltInType;
import org.emoflon.neo.emsl.eMSL.DataType;

import java.util.Objects;

public class DataTypeAssert extends AbstractAssert<DataTypeAssert, DataType> {
    protected DataTypeAssert(DataType type) {
        super(type, DataTypeAssert.class);
    }

    public static DataTypeAssert assertThat(DataType actual) {
        return new DataTypeAssert(actual);
    }

    public DataTypeAssert hasBuiltInReferenceType(BuiltInDataTypes expectedType) {
        isNotNull();

        if (!(actual instanceof BuiltInType)) {
            failWithMessage(
                    "Expected type to be an instance of <BuiltInType> but was <%s>",
                    actual.getClass().getSimpleName());
        }

        BuiltInType builtInType = (BuiltInType) actual;
        BuiltInDataTypes actualType = builtInType.getReference();
        if (!Objects.equals(actualType, expectedType)) {
            failWithMessage(
                    "Expected reference type to be <%s> but was <%s>", expectedType, actualType);
        }

        return this;
    }
}
