package tools.vitruv.neojoin.aqr;

import org.eclipse.emf.ecore.EClass;
import org.jspecify.annotations.Nullable;

/**
 * Source class together with an alias under which it can be referenced.
 *
 * @param clazz source class
 * @param alias alias for references
 */
public record AQRFrom(
    EClass clazz,
    @Nullable String alias
) {}
