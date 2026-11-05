package tools.vitruv.neojoin.aqr;

import org.jetbrains.annotations.Contract;

import java.util.function.Supplier;

/**
 * This exception is thrown when trying to construct an AQR from an invalid query.
 */
public class AQRInvariantViolatedException extends RuntimeException {

    public AQRInvariantViolatedException() {
        super();
    }

    public AQRInvariantViolatedException(String message) {
        super(message);
    }

    @Contract("-> fail")
    public static <T> T invariantFailed() {
        throw new AQRInvariantViolatedException();
    }

    @Contract("_ -> fail")
    public static <T> T invariantFailed(String message) {
        throw new AQRInvariantViolatedException(message);
    }

    @Contract("true -> _; false -> fail")
    public static void invariant(boolean condition) {
        if (!condition) {
            throw new AQRInvariantViolatedException();
        }
    }

    @Contract("true, _ -> _; false, _ -> fail")
    public static void invariant(boolean condition, String message) {
        if (!condition) {
            throw new AQRInvariantViolatedException(message);
        }
    }

    @Contract("true, _ -> _; false, _ -> fail")
    public static void invariant(boolean condition, Supplier<String> message) {
        if (!condition) {
            throw new AQRInvariantViolatedException(message.get());
        }
    }

}
