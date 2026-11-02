package tools.vitruv.neojoin.validation;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.validation.Check;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.AstUtils;
import tools.vitruv.neojoin.utils.EMFUtils;
import tools.vitruv.neojoin.utils.FeatureInfo;
import tools.vitruv.neojoin.utils.FeatureInfoResolver;
import tools.vitruv.neojoin.utils.FeatureTypeResolver;
import tools.vitruv.neojoin.utils.ImplicitClassCollector;
import tools.vitruv.neojoin.utils.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates the super classes that {@link tools.vitruv.neojoin.aqr.AQRBuilder} links to implicit target classes.
 */
public class ImplicitSuperClassValidator extends ComposableValidator {

    @Inject
    private ExpressionHelper expressionHelper;

    /**
     * Checks the implicit super class of implicit target classes: it must be unambiguous, and the implicit class
     * should have all features of it.
     * <p>
     * {@code AQRBuilder} links these classes (they cannot declare {@code extends}) to the target class of their
     * nearest source super class that has one: a query that selects exactly this source class (single {@code from},
     * no joins, no grouping, including subqueries), or an implicit target class. It fails if several target classes
     * exist on the nearest level of the source class hierarchy, so this is reported here instead. The corresponding
     * check in {@code AQRBuilder} stays as the safeguard for queries that were not validated (it decides which class
     * is linked, while this method only reports the ambiguity to the user).
     * <p>
     * An implicit class has the features of its source class, so it cannot override features of the candidate that
     * are not features of the source class. {@code AQRBuilder} does not link the candidate then
     * (the class would violate the rule that sub classes override all inherited features), which is reported as a
     * warning here.
     * <p>
     * The implicit classes are determined from the AST like in {@link FeatureValidator#checkAmbiguousImplicitFeatureTypes}: classes of
     * references (copied or inferred) for which no query selects the referenced source class. The issues are reported
     * at the feature that references the class, or at the query if the references are copied.
     */
    @Check
    public void checkAmbiguousImplicitSuperClasses(ViewTypeDefinition viewType) {
        var queriesBySourceClass = AstUtils.mapSourceClassesToQueries(viewType, expressionHelper);
        var typeResolver = new FeatureTypeResolver(queriesBySourceClass, expressionHelper);
        var featureInfoResolver = new FeatureInfoResolver(typeResolver, expressionHelper);

        var implicitClasses = new ImplicitClassCollector(typeResolver, expressionHelper).collectImplicitClasses(viewType);

        implicitClasses.forEach((sourceClass, cause) -> {
            var candidates = findNearestSuperClassCandidates(sourceClass, queriesBySourceClass, implicitClasses.keySet());
            if (candidates.size() > 1) {
                error(
                    "Ambiguous super class for implicit target class of source class '%s'. Possible candidates: %s".formatted(
                        sourceClass.getName(), candidates.stream().map(Pair::left).collect(Collectors.joining(", "))
                    ),
                    cause,
                    null
                );
            } else if (candidates.size() == 1 && candidates.get(0).right() != null) {
                checkImplicitClassOverridesAllFeatures(
                    sourceClass, candidates.get(0).left(), candidates.get(0).right(), cause, featureInfoResolver
                );
            }
        });
    }

    private void checkImplicitClassOverridesAllFeatures(
        EClass sourceClass,
        String superClassName,
        Query superClass,
        EObject cause,
        FeatureInfoResolver featureInfoResolver
    ) {
        var superClassFeatures = featureInfoResolver.getEffectiveFeatureInfos(superClass);
        if (superClassFeatures == null) {
            return; // features cannot be determined: other errors will be reported
        }

        var ownNames = sourceClass.getEAllStructuralFeatures().stream()
            .map(EStructuralFeature::getName)
            .collect(Collectors.toSet());
        var missingNames = superClassFeatures.stream()
            .map(FeatureInfo::name)
            .distinct()
            .filter(name -> !ownNames.contains(name))
            .toList();
        if (!missingNames.isEmpty()) {
            warning(
                "Implicit target class of source class '%s' is not linked to super class '%s', because it does not override: %s".formatted(
                    sourceClass.getName(), superClassName, String.join(", ", missingNames)
                ),
                cause,
                null
            );
        }
    }

    /**
     * Returns the target classes (name and query, the query is {@code null} for an implicit class) of the nearest
     * level of super classes of the given source class (breadth first) that has any, sorted by name.
     */
    private List<Pair<String, @Nullable Query>> findNearestSuperClassCandidates(
        EClass sourceClass,
        Map<EClass, Set<Query>> queriesBySourceClass,
        Set<EClass> implicitClasses
    ) {
        return EMFUtils.findInNearestSuperClasses(
            sourceClass, superClass -> {
                var candidates = queriesBySourceClass.getOrDefault(superClass, Set.of()).stream()
                    .filter(q -> superClass.equals(AstUtils.getSingleSourceClass(q, expressionHelper)))
                    .map(q -> new Pair<String, @Nullable Query>(AstUtils.getTargetName(q, expressionHelper), q))
                    .collect(Collectors.toCollection(ArrayList::new));
                if (implicitClasses.contains(superClass)) {
                    candidates.add(new Pair<>(superClass.getName(), null));
                }
                return candidates;
            }
        ).stream().sorted(Comparator.comparing(Pair::left)).toList();
    }

}
