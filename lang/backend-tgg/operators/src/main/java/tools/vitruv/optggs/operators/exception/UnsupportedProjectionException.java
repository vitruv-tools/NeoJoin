package tools.vitruv.optggs.operators.exception;

import tools.vitruv.neojoin.aqr.AQRFeature;

public class UnsupportedProjectionException extends RuntimeException {
    public UnsupportedProjectionException(AQRFeature feature) {
        super(String.format("The feature projection %s is not supported", feature));
    }

    public UnsupportedProjectionException(String message) {
        super(message);
    }
}
