package tools.vitruv.neojoin.utils;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.neojoin.ast.ConcreteBody;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.FeatureType.ImplicitReferenceType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Determines the source classes for which {@link tools.vitruv.neojoin.aqr.AQRBuilder} creates an
 * implicit target
 * class, from the AST: the classes of references (copied or inferred) for which no query selects the referenced source
 * class, and (transitively) the classes of their references.
 */
public class ImplicitClassCollector {

    private final ExpressionHelper expressionHelper;
    private final FeatureTypeResolver typeResolver;

    public ImplicitClassCollector(FeatureTypeResolver typeResolver, ExpressionHelper expressionHelper) {
        this.typeResolver = typeResolver;
        this.expressionHelper = expressionHelper;
    }

    /**
     * @return the implicit source classes, each with the first AST element that causes it: the feature that
     *     references it, or the query if the references are copied
     */
    public Map<EClass, EObject> collectImplicitClasses(ViewTypeDefinition viewType) {
        var implicitClasses = new LinkedHashMap<EClass, EObject>();
        AstUtils.getAllQueries(viewType).forEach(query -> collectImplicitClasses(query, implicitClasses));
        return implicitClasses;
    }

    private void collectImplicitClasses(Query query, Map<EClass, EObject> implicitClasses) {
        var body = AstUtils.getBody(query);
        if (body == null) {
            var sourceClass = AstUtils.getCopiedSourceClass(query, expressionHelper);
            if (sourceClass != null) {
                collectImplicitReferenceTypes(sourceClass, query, implicitClasses);
            }
        } else if (body instanceof ConcreteBody concreteBody) {
            for (var feature : concreteBody.getFeatures()) {
                if (feature.getType() == null && feature.getSubQuery() == null
                    && typeResolver.resolve(feature) instanceof ImplicitReferenceType type) {
                    addImplicitClass(type.sourceClass(), feature, implicitClasses);
                }
            }
        }
    }

    private void addImplicitClass(EClass sourceClass, EObject cause, Map<EClass, EObject> implicitClasses) {
        if (implicitClasses.putIfAbsent(sourceClass, cause) == null) { // also prevents infinite recursion
            collectImplicitReferenceTypes(sourceClass, cause, implicitClasses);
        }
    }

    private void collectImplicitReferenceTypes(
        EClass copiedClass,
        EObject cause,
        Map<EClass, EObject> implicitClasses
    ) {
        for (var reference : copiedClass.getEAllReferences()) {
            if (typeResolver.resolveSourceClass(reference.getEReferenceType()) instanceof ImplicitReferenceType) {
                addImplicitClass(reference.getEReferenceType(), cause, implicitClasses);
            }
        }
    }

}
