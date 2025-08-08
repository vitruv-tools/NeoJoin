package tools.vitruv.neojoin.aqr;

import org.eclipse.emf.ecore.EPackage;

/**
 * Imported package
 *
 * @param pack  package
 * @param alias name under which the package can be referenced in the query
 */
public record AQRImport(
    EPackage pack,
    String alias
) {}
