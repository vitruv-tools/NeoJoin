package tools.vitruv.neojoin.aqr;

import org.eclipse.emf.common.util.URI;

/**
 * Info for the generated target meta-model.
 *
 * @param name name of the model
 * @param uri  uri of the model
 */
public record AQRExport(
    String name,
    URI uri
) {}
