package tools.vitruv.neojoin.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class ResultTest {

    @Test
    void executeCatchingNothing() {
        var expectedValue = "test value";
        var result = Result.executeCatching(IOException.class, () -> expectedValue);

        assertEquals(Result.of(expectedValue), result);
    }

    @Test
    void executeCatchingCheckedException() {
        var expectedException = new  IOException("This is an  example exception for testing.");
        var result = Result.executeCatching(IOException.class, () -> {
            throw expectedException;
        });

        assertInstanceOf(Result.Failure.class, result);
        var actualException = ((Result.Failure<?,  IOException>) result).throwable();
        assertInstanceOf(IOException.class, actualException);
        assertEquals(expectedException.getMessage(), actualException.getMessage());
    }

    @Test
    void executeCatchingRuntimeException() {
        var expectedException = new  RuntimeException("This is an  example exception for testing.");
        var result = Result.executeCatching(RuntimeException.class, () -> {
            throw expectedException;
        });

        assertInstanceOf(Result.Failure.class, result);
        var actualException = ((Result.Failure<?,  RuntimeException>) result).throwable();
        assertInstanceOf(RuntimeException.class, actualException);
        assertEquals(expectedException.getMessage(), actualException.getMessage());
    }


    @Test
    void executeCatchingButDontCatchUnexpectedException() {
        assertThrowsExactly(IllegalArgumentException.class, () ->
            Result.executeCatching(IOException.class, () -> {
                throw new IllegalArgumentException();
            }));

        assertThrowsExactly(IllegalStateException.class, () ->
            Result.executeCatching(IllegalArgumentException.class, () -> {
                throw new IllegalStateException();
            }));
    }
}

