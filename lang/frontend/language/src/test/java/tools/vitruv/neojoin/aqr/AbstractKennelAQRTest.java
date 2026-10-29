package tools.vitruv.neojoin.aqr;

import tools.vitruv.neojoin.AbstractKennelTest;
import tools.vitruv.neojoin.jvmmodel.ExpressionHelper;

/**
 * Base class for AQR tests with the kennel meta-model that build the AQR directly with the {@link AQRBuilder}. In
 * contrast to {@link #parse}, the query may have validation issues, so this can test that the {@link AQRBuilder} fails
 * with an {@link AQRInvariantViolatedException} instead of building an inconsistent AQR.
 */
abstract class AbstractKennelAQRTest extends AbstractKennelTest {

    protected AQR buildAQR(String query) {
        var result = internalParse("""
            export view to "http://example.com/view"

            import "http://example.org/kennel"
            """ + query);
        return new AQRBuilder(result.left(), getInjector().getInstance(ExpressionHelper.class)).build();
    }

}
