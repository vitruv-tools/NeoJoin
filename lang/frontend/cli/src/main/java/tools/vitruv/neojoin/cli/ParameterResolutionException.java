package tools.vitruv.neojoin.cli;

class ParameterResolutionException extends RuntimeException {

    ParameterResolutionException(String message) {
        super(message);
    }

    ParameterResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
