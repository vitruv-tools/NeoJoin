package tools.vitruv.neojoin.parse.testutils;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.xtext.xbase.XExpression;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.jvmmodel.TypeInfo;
import tools.vitruv.neojoin.jvmmodel.TypeResolutionException;

import static org.assertj.core.api.Assertions.fail;


public final class TypeTestUtils {
    private static final String FAILED_TO_RETRIEVE_TYPE_MESSAGE_PREFIX = "Failed to retrieve inferred type of feature:";

    private TypeTestUtils() {}


    public static EClassifier getClassifierOrFail(TypeInfo typeInfo) {
        EClassifier classifier = typeInfo.classifier();

        return classifier == null ? fail("%s Classifier was null.".formatted(FAILED_TO_RETRIEVE_TYPE_MESSAGE_PREFIX))
            : classifier;
    }

    public static TypeInfo inferredTypeOrFail(ExpressionHelper expressionHelper, XExpression expression) {
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
