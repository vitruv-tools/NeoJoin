package tools.vitruv.neojoin.utils;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.ast.ConcreteFeature;
import tools.vitruv.neojoin.ast.Feature;
import tools.vitruv.neojoin.ast.FeatureOp;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.FeatureType.AmbiguousReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.AttributeType;
import tools.vitruv.neojoin.utils.FeatureType.EcoreReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.ImplicitReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.QueryReferenceType;
import tools.vitruv.neojoin.utils.FeatureType.ReferenceType;

import java.util.Map;
import java.util.Set;

/**
 * Determines the {@link FeatureType type} of features from the AST, without creating anything. The rules are the ones
 * that {@link tools.vitruv.neojoin.aqr.AQRBuilder} applies when it creates the features of a target
 * class.
 */
public class FeatureTypeResolver {

    private final ExpressionHelper expressionHelper;
    private final Map<EClass, Set<Query>> queriesBySourceClass;
    private final Map<String, Query> abstractQueriesByName;

    public FeatureTypeResolver(ViewTypeDefinition viewType, ExpressionHelper expressionHelper) {
        this(viewType, AstUtils.mapSourceClassesToQueries(viewType, expressionHelper), expressionHelper);
    }

    /**
     * @param queriesBySourceClass see {@link AstUtils#mapSourceClassesToQueries}
     */
    public FeatureTypeResolver(
        ViewTypeDefinition viewType,
        Map<EClass, Set<Query>> queriesBySourceClass,
        ExpressionHelper expressionHelper
    ) {
        this.queriesBySourceClass = queriesBySourceClass;
        this.abstractQueriesByName = AstUtils.mapAbstractQueriesByName(viewType, expressionHelper);
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
     * Determines the type of a reference to the given source class: the query that selects it, or the explicit
     * abstract class of the same name (see {@link #findExplicitAbstractQuery}), or an implicitly created target class
     * if no query selects it. Classes of the Ecore meta-model are not copied into the view type, so the type of a
     * reference to an unselected Ecore class is the Ecore class itself.
     */
    public ReferenceType resolveSourceClass(EClass sourceClass) {
        var candidates = queriesBySourceClass.get(sourceClass);
        if (candidates == null || candidates.isEmpty()) {
            if (sourceClass.getEPackage() == EcorePackage.eINSTANCE) {
                return new EcoreReferenceType(sourceClass);
            }
            var abstractQuery = findExplicitAbstractQuery(sourceClass);
            return abstractQuery != null
                ? new QueryReferenceType(abstractQuery)
                : new ImplicitReferenceType(sourceClass);
        } else if (candidates.size() == 1) {
            return new QueryReferenceType(candidates.iterator().next());
        } else {
            return new AmbiguousReferenceType(sourceClass, candidates);
        }
    }

    /**
     * Finds the explicit abstract class that is used instead of an implicit target class for the given source class:
     * if no query selects the source class, an abstract class without source that has the name of the source class
     * is the target class of the source class, so that no second class with the same name is created.
     *
     * @return the abstract query or {@code null} if the source class is selected by a query or if there is no such
     *     abstract class
     */
    public @Nullable Query findExplicitAbstractQuery(EClass sourceClass) {
        var candidates = queriesBySourceClass.get(sourceClass);
        return candidates == null || candidates.isEmpty() ? abstractQueriesByName.get(sourceClass.getName()) : null;
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

        var inferredType = AstUtils.inferTypeOrNull(feature.getExpression(), expressionHelper);
        if (inferredType == null || inferredType.classifier() == null) {
            return null;
        }

        if (inferredType.classifier() instanceof EDataType inferredDataType) {
            if (feature.getOp() == FeatureOp.COPY) {
                var copied = AstUtils.getReferencedFeatureOrNull(feature.getExpression(), expressionHelper);
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

}
