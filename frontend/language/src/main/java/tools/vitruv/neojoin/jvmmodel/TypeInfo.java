package tools.vitruv.neojoin.jvmmodel;

import org.eclipse.emf.ecore.EClassifier;
import org.jspecify.annotations.Nullable;

/**
 * Inferred type of an expression.
 *
 * @param classifier inferred source classifier or {@code null} if the expression was {@code null}
 * @param isMany     whether the expression is a list type or not
 */
public record TypeInfo(
    @Nullable
    EClassifier classifier,
    boolean isMany
) {}
