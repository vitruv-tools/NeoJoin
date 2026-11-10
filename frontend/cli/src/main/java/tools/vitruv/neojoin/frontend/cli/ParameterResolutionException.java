package tools.vitruv.neojoin.frontend.cli;

class ParameterResolutionException extends RuntimeException {

    ParameterResolutionException(String message) {
        super(message);
    }

    ParameterResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
