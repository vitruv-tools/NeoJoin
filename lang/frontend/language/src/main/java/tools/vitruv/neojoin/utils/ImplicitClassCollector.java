package tools.vitruv.neojoin.utils;

import org.eclipse.emf.ecore.EClass;
import tools.vitruv.neojoin.ast.ConcreteBody;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.FeatureType.ImplicitReferenceType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Determines the source classes for which {@link tools.vitruv.neojoin.aqr.AQRBuilder} creates an implicit target
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
     * @return the implicit source classes, each with the first query that causes it
     */
    public Map<EClass, Query> collectImplicitClasses(ViewTypeDefinition viewType) {
        var implicitClasses = new LinkedHashMap<EClass, Query>();
        AstUtils.getAllQueries(viewType).forEach(query -> collectImplicitClasses(query, implicitClasses));
        return implicitClasses;
    }

    private void collectImplicitClasses(Query query, Map<EClass, Query> implicitClasses) {
        var body = AstUtils.getBody(query);
        if (body == null) {
            var sourceClass = AstUtils.getCopiedSourceClass(query, expressionHelper);
            if (sourceClass != null) {
                collectImplicitReferenceTypes(sourceClass, query, implicitClasses);
            }
        } else if (body instanceof ConcreteBody concreteBody) {
            concreteBody.getFeatures().stream()
                .filter(f -> f.getType() == null && f.getSubQuery() == null)
                .map(typeResolver::resolve)
                .filter(ImplicitReferenceType.class::isInstance)
                .map(ImplicitReferenceType.class::cast)
                .forEach(type -> addImplicitClass(type.sourceClass(), query, implicitClasses));
        }
    }

    private void addImplicitClass(EClass sourceClass, Query cause, Map<EClass, Query> implicitClasses) {
        if (implicitClasses.putIfAbsent(sourceClass, cause) == null) { // also prevents infinite recursion
            collectImplicitReferenceTypes(sourceClass, cause, implicitClasses);
        }
    }

    private void collectImplicitReferenceTypes(EClass copiedClass, Query cause, Map<EClass, Query> implicitClasses) {
        for (var reference : copiedClass.getEAllReferences()) {
            if (typeResolver.resolveSourceClass(reference.getEReferenceType()) instanceof ImplicitReferenceType) {
                addImplicitClass(reference.getEReferenceType(), cause, implicitClasses);
            }
        }
    }

}
