package tools.vitruv.neojoin.aqr;

import org.eclipse.emf.ecore.EDataType;
/**
 * Imported package
 *
 * @param alias name under which the parameters can be referenced in the query
 */
public record AQRParameter(
    String alias,
    EDataType type
) {}
