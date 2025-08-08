package tools.vitruv.neojoin.utils;

import org.jetbrains.annotations.Contract;

import java.util.function.Supplier;

public class Assertions {

    private Assertions() {}

    @Contract("-> fail")
    public static <T> T fail() {
        throw new AssertionError();
    }

    @Contract("_ -> fail")
    public static <T> T fail(String message) {
        throw new AssertionError(message);
    }

    @Contract("true -> _; false -> fail")
    public static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }

    @Contract("true, _ -> _; false, _ -> fail")
    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @Contract("true, _ -> _; false, _ -> fail")
    public static void check(boolean condition, Supplier<String> message) {
        if (!condition) {
            throw new AssertionError(message.get());
        }
    }

    @Contract("true -> _; false -> fail")
    public static void require(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
    }

    @Contract("true, _ -> _; false, _ -> fail")
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    @Contract("true, _ -> _; false, _ -> fail")
    public static void require(boolean condition, Supplier<String> message) {
        if (!condition) {
            throw new IllegalArgumentException(message.get());
        }
    }

}
