package tools.vitruv.neojoin.aqr;

import org.eclipse.emf.ecore.EClassifier;

/**
 * A parameter declared in a NeoJoin query.
 *
 * @param alias  name under which the parameter can be referenced in the query
 * @param type   the declared type of the parameter
 * @param isList whether the parameter holds a list of values
 */
public record AQRParameter(
    String alias,
    EClassifier type,
    boolean isList
) {}
