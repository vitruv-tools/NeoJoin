package tools.vitruv.neojoin.aqr;

import org.eclipse.emf.ecore.EClassifier;

/**
 * Imported package
 *
 * @param alias name under which the parameters can be referenced in the query
 */
public record AQRParameter(
    String alias,
    EClassifier type,
    boolean isList
) {}
