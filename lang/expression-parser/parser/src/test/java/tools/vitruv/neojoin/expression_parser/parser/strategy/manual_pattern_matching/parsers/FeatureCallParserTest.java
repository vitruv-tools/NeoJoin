package tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.junit.jupiter.api.Test;

import tools.vitruv.neojoin.expression_parser.model.FeatureCall;
import tools.vitruv.neojoin.expression_parser.model.ReferenceOperator;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.JvmFormalParameterFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.JvmTypeFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.JvmTypeReferenceFixtures;
import tools.vitruv.neojoin.expression_parser.parser.strategy.manual_pattern_matching.fixtures.XFeatureCallFixtures;

import java.util.Optional;

class FeatureCallParserTest {
    private static final FeatureCallParser parser = new FeatureCallParser();

    @Test
    public void parseEmptyFeatureCall() {
        // given
        final JvmFormalParameter emptyFormalParameter =
                JvmFormalParameterFixtures.createJvmFormalParameter();
        final XFeatureCall featureCall = XFeatureCallFixtures.createXFeatureCall();
        featureCall.setFeature(emptyFormalParameter);

        // when
        final Optional<ReferenceOperator> resultOptional = parser.parse(null, featureCall);

        // then
        assertTrue(resultOptional.isPresent());

        final ReferenceOperator result = resultOptional.get();
        assertInstanceOf(FeatureCall.class, result);

        final FeatureCall resultFeatureCall = (FeatureCall) result;
        assertNull(resultFeatureCall.getIdentifier());
        assertNull(resultFeatureCall.getSimpleName());
        assertNull(resultFeatureCall.getFollowingOperator());
    }

    @Test
    public void parseNonEmptyFeatureCall() {
        // given
        final JvmType jvmType = JvmTypeFixtures.createJvmType("my.test.package.Car", "Car");
        final JvmTypeReference jvmTypeReference =
                JvmTypeReferenceFixtures.createJvmTypeReference(jvmType);
        final JvmFormalParameter formalParameter =
                JvmFormalParameterFixtures.createJvmFormalParameter(jvmTypeReference);
        final XFeatureCall featureCall = XFeatureCallFixtures.createXFeatureCall();
        featureCall.setFeature(formalParameter);

        // when
        final Optional<ReferenceOperator> resultOptional = parser.parse(null, featureCall);

        // then
        assertTrue(resultOptional.isPresent());

        final ReferenceOperator result = resultOptional.get();
        assertInstanceOf(FeatureCall.class, result);

        final FeatureCall resultAsFeatureCall = (FeatureCall) result;
        assertEquals("my.test.package.Car", resultAsFeatureCall.getIdentifier());
        assertEquals("Car", resultAsFeatureCall.getSimpleName());
        assertNull(resultAsFeatureCall.getFollowingOperator());
    }
}
