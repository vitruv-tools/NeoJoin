package tools.vitruv.neojoin.utils;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import tools.vitruv.neojoin.ast.Query;

import java.util.Set;

/**
 * The type of a feature as determined from the AST, see {@link FeatureTypeResolver}.
 */
public sealed interface FeatureType {

    /**
     * @param type the data type of the attribute
     */
    record AttributeType(EDataType type) implements FeatureType {}

    /**
     * The type of a reference.
     */
    sealed interface ReferenceType extends FeatureType {}

    /**
     * @param query the {@link Query} that creates the target class which is the type of the reference
     */
    record QueryReferenceType(Query query) implements ReferenceType {}

    /**
     * @param sourceClass the source class that is copied to an implicitly created target class which is the type of
     *                    the reference
     */
    record ImplicitReferenceType(EClass sourceClass) implements ReferenceType {}

    /**
     * @param ecoreClass the class of the Ecore meta-model (e.g. {@code EObject}) that no query selects: it is not
     *                   copied into the view type, so the reference keeps pointing to it
     */
    record EcoreReferenceType(EClass ecoreClass) implements ReferenceType {}

    /**
     * @param sourceClass the source class that is selected by multiple queries
     * @param candidates  the queries that select the source class
     */
    record AmbiguousReferenceType(EClass sourceClass, Set<Query> candidates) implements ReferenceType {}

}
