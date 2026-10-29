package tools.vitruv.neojoin.utils;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.XExpression;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.ast.ConcreteFeature;
import tools.vitruv.neojoin.ast.Feature;
import tools.vitruv.neojoin.ast.FeatureOp;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.jvmmodel.TypeInfo;
import tools.vitruv.neojoin.jvmmodel.TypeResolutionException;
import tools.vitruv.neojoin.utils.FeatureType.AmbiguousReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.AttributeType;
import tools.vitruv.neojoin.utils.FeatureType.ImplicitReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.QueryReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.ReferenceType;

import java.util.Map;
import java.util.Set;

/**
 * Determines the {@link FeatureType type} of features from the AST, without creating anything. The rules are the ones
 * that {@link tools.vitruv.neojoin.aqr.AQRBuilder} applies when it creates the features of a target class.
 */
public class FeatureTypeResolver {

    private final ExpressionHelper expressionHelper;
    private final Map<EClass, Set<Query>> queriesBySourceClass;

    public FeatureTypeResolver(ViewTypeDefinition viewType, ExpressionHelper expressionHelper) {
        this(AstUtils.mapSourceClassesToQueries(viewType, expressionHelper), expressionHelper);
    }

    /**
     * @param queriesBySourceClass see {@link AstUtils#mapSourceClassesToQueries}
     */
    public FeatureTypeResolver(Map<EClass, Set<Query>> queriesBySourceClass, ExpressionHelper expressionHelper) {
        this.queriesBySourceClass = queriesBySourceClass;
        this.expressionHelper = expressionHelper;
    }

    /**
     * Determines the type of a declared feature: the explicit type if given, otherwise the subquery or the type of
     * the expression.
     *
     * @return the type or {@code null} if it cannot be determined (e.g. because of other errors)
     */
    public @Nullable FeatureType resolve(Feature feature) {
        if (feature.getType() != null) {
            return resolveExplicitType(feature.getType());
        }
        if (feature instanceof ConcreteFeature concreteFeature && concreteFeature.getExpression() != null) {
            return resolveInferredType(concreteFeature);
        }
        return null;
    }

    /**
     * Determines the type of a feature that is copied implicitly from the source class.
     *
     * @return the type or {@code null} if the feature is neither an attribute nor a reference
     */
    public @Nullable FeatureType resolveCopied(EStructuralFeature feature) {
        if (feature instanceof EAttribute attribute) {
            return new AttributeType(attribute.getEAttributeType());
        } else if (feature instanceof EReference reference) {
            return resolveSourceClass(reference.getEReferenceType());
        }
        return null;
    }

    /**
     * Determines the type of a reference to the given source class: the query that selects it, or an implicitly
     * created target class if no query selects it.
     */
    public ReferenceType resolveSourceClass(EClass sourceClass) {
        var candidates = queriesBySourceClass.get(sourceClass);
        if (candidates == null || candidates.isEmpty()) {
            return new ImplicitReferenceType(sourceClass);
        } else if (candidates.size() == 1) {
            return new QueryReferenceType(candidates.iterator().next());
        } else {
            return new AmbiguousReferenceType(sourceClass, candidates);
        }
    }

    private static @Nullable FeatureType resolveExplicitType(EObject explicitType) {
        if (explicitType.eIsProxy()) {
            return null;
        } else if (explicitType instanceof EDataType dataType) {
            return new AttributeType(dataType);
        } else if (explicitType instanceof Query query) {
            return new QueryReferenceType(query);
        }
        return null;
    }

    private @Nullable FeatureType resolveInferredType(ConcreteFeature feature) {
        if (feature.getSubQuery() != null) {
            return new QueryReferenceType(feature.getSubQuery());
        }

        var inferredType = inferTypeOrNull(feature.getExpression());
        if (inferredType == null || inferredType.classifier() == null) {
            return null;
        }

        if (inferredType.classifier() instanceof EDataType inferredDataType) {
            if (feature.getOp() == FeatureOp.COPY) {
                var copied = getCopiedFeatureOrNull(feature.getExpression());
                if (copied != null && copied.getEType() instanceof EDataType copiedDataType) {
                    return new AttributeType(copiedDataType);
                }
            }
            return new AttributeType(inferredDataType);
        } else if (inferredType.classifier() instanceof EClass inferredClass) {
            return resolveSourceClass(inferredClass);
        }
        return null;
    }

    private @Nullable TypeInfo inferTypeOrNull(XExpression expression) {
        try {
            return expressionHelper.inferEType(expression);
        } catch (TypeResolutionException e) {
            return null; // ignore: will be handled by type checking
        }
    }

    private @Nullable EStructuralFeature getCopiedFeatureOrNull(XExpression expression) {
        try {
            return expressionHelper.getFeatureOrNull(expression);
        } catch (TypeResolutionException e) {
            return null; // ignore: will be handled by type checking
        }
    }

}
