package tools.vitruv.neojoin.validation;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.Check;
import tools.vitruv.neojoin.ast.Query;
import tools.vitruv.neojoin.ast.ViewTypeDefinition;
import tools.vitruv.neojoin.common.Pair;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.utils.FeatureInfo;
import tools.vitruv.neojoin.utils.FeatureInfoResolver;
import tools.vitruv.neojoin.utils.ImplicitSuperClassFinder;

import java.util.stream.Collectors;

/**
 * Validates the super classes that {@link tools.vitruv.neojoin.aqr.AQRBuilder} links to implicit
 * target classes
 * (they cannot declare {@code extends}): the nearest source super class that has a target class is the candidate.
 * <p>
 * The implicit classes are determined from the AST like in {@link FeatureValidator#checkAmbiguousImplicitFeatureTypes}:
 * classes of references (copied or inferred) for which no query selects the referenced source class. The issues are
 * reported at the feature that references the class, or at the query if the references are copied.
 */
public class ImplicitSuperClassValidator extends ComposableValidator {

    @Inject
    private ExpressionHelper expressionHelper;

    /**
     * Checks that an implicit target class has an unambiguous super class.
     * <p>
     * {@code AQRBuilder} fails if several target classes exist on the nearest level of the source class hierarchy, so
     * this is reported here instead. The corresponding check in {@code AQRBuilder} stays as the safeguard for queries
     * that were not validated (it decides which class is linked, while this method only reports the ambiguity to the
     * user).
     */
    @Check
    public void checkAmbiguousImplicitSuperClasses(ViewTypeDefinition viewType) {
        var finder = new ImplicitSuperClassFinder(viewType, expressionHelper);
        finder.getImplicitClasses().forEach((sourceClass, cause) -> {
            var candidates = finder.findCandidates(sourceClass);
            if (candidates.size() > 1) {
                error(
                    ("Ambiguous super class for implicit target class of source class '%s'. "
                        + "Possible candidates: %s").formatted(
                        sourceClass.getName(), candidates.stream().map(Pair::left).collect(Collectors.joining(", "))
                    ),
                    cause,
                    null
                );
            }
        });
    }

    /**
     * Warns if an implicit target class is not linked to its super class, because it has no equivalent feature (same
     * name, type and modifiers) for each feature of the candidate.
     * <p>
     * An implicit class has the features of its source class. An override takes the type and the modifiers of the
     * overridden feature and a class must override all inherited features, so {@code AQRBuilder} does not link the
     * candidate if the implicit copy of a source feature would change or a feature of the candidate is missing.
     */
    @Check
    public void checkImplicitClassesMatchSuperClasses(ViewTypeDefinition viewType) {
        var finder = new ImplicitSuperClassFinder(viewType, expressionHelper);
        var featureInfoResolver = finder.createFeatureInfoResolver();
        finder.getImplicitClasses().forEach((sourceClass, cause) -> {
            var candidates = finder.findCandidates(sourceClass);
            // an implicit candidate (no query) always matches: both have the features of the source classes
            if (candidates.size() == 1 && candidates.get(0).right() != null) {
                checkImplicitClassMatchesSuperClass(
                    sourceClass, candidates.get(0).left(), candidates.get(0).right(), cause, featureInfoResolver
                );
            }
        });
    }

    private void checkImplicitClassMatchesSuperClass(
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

        var implicitFeatures = featureInfoResolver.getCopiedFeatureInfos(sourceClass);
        var missingNames = superClassFeatures.stream()
            .filter(superFeature -> implicitFeatures.stream().noneMatch(
                feature -> feature.name().equals(superFeature.name()) && feature.hasSameTypeAndModifiers(superFeature)
            ))
            .map(FeatureInfo::name)
            .distinct()
            .toList();
        if (!missingNames.isEmpty()) {
            warning(
                ("Implicit target class of source class '%s' is not linked to super class '%s', "
                    + "because it has no matching feature (same name, type and modifiers) for: %s").formatted(
                    sourceClass.getName(), superClassName, String.join(", ", missingNames)
                ),
                cause,
                null
            );
        }
    }

}
