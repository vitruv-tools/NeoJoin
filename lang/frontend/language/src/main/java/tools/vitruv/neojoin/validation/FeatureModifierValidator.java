package tools.vitruv.neojoin.validation;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.xtext.validation.Check;
import org.jspecify.annotations.Nullable;
import tools.vitruv.neojoin.ast.AstPackage;
import tools.vitruv.neojoin.ast.BooleanModifier;
import tools.vitruv.neojoin.ast.Feature;
import tools.vitruv.neojoin.ast.Modifier;
import tools.vitruv.neojoin.ast.MultiplicityBounds;
import tools.vitruv.neojoin.ast.MultiplicityExact;
import tools.vitruv.neojoin.ast.MultiplicityExpr;
import tools.vitruv.neojoin.ast.MultiplicityManyAtLeast;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;
import tools.vitruv.neojoin.jvmmodel.TypeResolutionException;
import tools.vitruv.neojoin.utils.AstUtils;

import java.util.stream.Collectors;

import static tools.vitruv.neojoin.utils.Assertions.fail;

public class FeatureModifierValidator extends ComposableValidator {

    @Inject
    private ExpressionHelper expressionHelper;

    @Check
    void checkEmptyModifierList(Feature feature) {
        if (feature.isHasModifiers() && feature.getModifiers().isEmpty()) {
            error("Empty modifier list", feature, AstPackage.Literals.FEATURE__MODIFIERS);
        }
    }

    @Check
    void checkDuplicates(Feature feature) {
        feature.getModifiers().stream()
            .collect(Collectors.groupingBy(FeatureModifierValidator::getModifierName))
            .values().stream()
            .filter(collisions -> collisions.size() > 1)
            .forEach(collisions -> {
                collisions.forEach(collision -> {
                    error("Duplicated modifier '%s'".formatted(getModifierName(collision)), collision, null);
                });
            });
    }

    private static String getModifierName(Modifier modifier) {
        if (modifier instanceof BooleanModifier booleanModifier) {
            return booleanModifier.getName();
        } else if (modifier instanceof MultiplicityExpr) {
            return "multiplicity";
        } else {
            return fail();
        }
    }

    @Check
    void checkApplicability(Feature feature) {
        if (feature.getModifiers().isEmpty()) {
            return;
        }

        var isAttribute = isAttribute(feature);
        if (isAttribute != null) {
            if (isAttribute) {
                feature.getModifiers().forEach(modifier -> {
                    if (modifier instanceof BooleanModifier booleanModifier && booleanModifier.getName().equals(
                        "containment")) {
                        error("Modifier 'containment' is not applicable to attributes", modifier, null);
                    }
                });
            } else {
                feature.getModifiers().forEach(modifier -> {
                    if (modifier instanceof BooleanModifier booleanModifier && booleanModifier.getName().equals("id")) {
                        error("Modifier 'id' is not applicable to references", modifier, null);
                    }
                });
            }
        }
    }

    private @Nullable Boolean isAttribute(Feature feature) {
        if (feature.getType() != null) {
            return feature.getType() instanceof EDataType;
        } else {
            try {
                var inferredType = expressionHelper.inferEType(feature.getExpression());
                if (inferredType != null) {
                    return inferredType.classifier() instanceof EDataType;
                }
            } catch (TypeResolutionException e) {
                // ignore: will be handled by type checking
            }
        }

        return null;
    }

    @Check
    void checkMultiplicity(Feature feature) {
        var explicitMultiplicity = AstUtils.findMultiplicityExpression(feature);
        if (explicitMultiplicity != null) {
            switch (explicitMultiplicity) {
                case MultiplicityBounds bounds -> {
                    if (bounds.getUpperBound() == 0) {
                        error(
                            "Upper bound must be at least 1",
                            explicitMultiplicity,
                            AstPackage.Literals.MULTIPLICITY_BOUNDS__UPPER_BOUND
                        );
                        return; // suppress duplicated errors
                    } else if (bounds.getLowerBound() > bounds.getUpperBound()) {
                        error("Lower bound must be less than upper bound", explicitMultiplicity, null);
                        return; // suppress duplicated errors
                    } else if (bounds.getLowerBound() == bounds.getUpperBound()) {
                        warning(
                            "Lower and upper bound are equal, consider using a single value instead",
                            explicitMultiplicity,
                            null
                        );
                    }
                }
                case MultiplicityManyAtLeast manyAtLeast -> {
                    if (manyAtLeast.getLowerBound() == 0) {
                        warning(
                            "Lower bound is 0, consider using '*' instead",
                            explicitMultiplicity,
                            AstPackage.Literals.MULTIPLICITY_MANY_AT_LEAST__LOWER_BOUND
                        );
                    } else if (manyAtLeast.getLowerBound() == 1) {
                        warning(
                            "Lower bound is 1, consider using '+' instead",
                            explicitMultiplicity,
                            AstPackage.Literals.MULTIPLICITY_MANY_AT_LEAST__LOWER_BOUND
                        );
                    }
                }
                case MultiplicityExact exact -> {
                    if (exact.getExact() == 0) {
                        error(
                            "Exact multiplicity must be at least 1",
                            explicitMultiplicity,
                            AstPackage.Literals.MULTIPLICITY_EXACT__EXACT
                        );
                        return; // suppress duplicated errors
                    }
                }
                default -> {}
            }

            var explicitIsMany = AstUtils.isManyMultiplicity(explicitMultiplicity);
            try {
                var inferredType = expressionHelper.inferEType(feature.getExpression());
                if (inferredType != null) {
                    var inferredIsMany = inferredType.isMany();
                    if (explicitIsMany && !inferredIsMany) {
                        error(
                            "Cannot assign a single value to a multi-valued feature",
                            feature,
                            AstPackage.Literals.FEATURE__EXPRESSION
                        );
                    } else if (!explicitIsMany && inferredIsMany) {
                        error(
                            "Cannot assign multiple values to a single-valued feature",
                            feature,
                            AstPackage.Literals.FEATURE__EXPRESSION
                        );
                    }
                }
            } catch (TypeResolutionException e) {
                // ignore: will be handled by type checking
            }
        }
    }

}
