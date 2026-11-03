package tools.vitruv.neojoin.utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;
import static tools.vitruv.neojoin.utils.Utils.collectFailingFast;
import static tools.vitruv.neojoin.utils.Utils.groupOrFail;
import static tools.vitruv.neojoin.utils.Utils.toMapFailOnDuplicates;

public class UtilsTest {

    @Test
    void collectFailingFastWithError() {
        var exception = new Exception("Message");
        Stream<Result<String, Exception>> streamWithError = Stream.of(
                Result.of(""),
                Result.of("foo"),
                Result.fail(exception),
                Result.of("bar")
        );

        var result = streamWithError.collect(collectFailingFast(toList()));

        assertEquals(Result.fail(exception), result);
    }

    @Test
    void collectFailingFastWithoutError() {
        var values = List.of("a", "b", "c", "d", "e", "f");
        var stream = values.stream().map(Result::<String, Exception>of);

        var result = stream.collect(collectFailingFast(toList()));

        assertEquals( Result.of(values), result);
    }

    @Test
    void groupOrFailWithError() {
        var exception = new Exception("grouping failed");
        Stream<Result<Pair<String, String>, Exception>> values = Stream.of(
            Result.of(Pair.of("a", "b")),
            Result.of(Pair.of("a", "c")),
            Result.of(Pair.of("b", "c")),
            Result.of(Pair.of("c", "d")),
            Result.fail(exception),
            Result.of(Pair.of("a", "d"))
        );


        var result = values.collect(groupOrFail());

        assertEquals(Result.fail(exception), result);
    }

    @Test
    void groupOrFailWithoutError() {
        Stream<Result<Pair<String, String>, Exception>> values = Stream.of(
            Result.of(Pair.of("a", "b")),
            Result.of(Pair.of("a", "c")),
            Result.of(Pair.of("b", "c")),
            Result.of(Pair.of("c", "d")),
            Result.of(Pair.of("a", "d"))
        );
        var expected = Result.of(Map.of(
            "a", List.of("b", "c", "d"),
            "b", List.of("c"),
            "c", List.of("d")
        ));

        var result = values.collect(groupOrFail());

        assertEquals(expected, result);
    }

    @Test
    void toMapFailOnDuplicatesWithError() {
        TriFunction<String, String, String, Exception> createException = (key, value1, value2) ->
            new Exception(String.format("duplicate occurred: key: %s, values: %s, %s", key, value1, value2));
        var values = Stream.of(
            Pair.of("a", "b"),
            Pair.of("a", "c"),
            Pair.of("b", "c"),
            Pair.of("c", "d"),
            Pair.of("a", "d")
        );

        var result = values.collect(toMapFailOnDuplicates(createException));
        var expectedException = createException.apply("a", "b", "c");

        assertInstanceOf(Result.Failure.class, result);
        assertEquals(expectedException.getMessage(),  ((Result.Failure) result).throwable().getMessage());
    }

    @Test
    void toMapFailOnDuplicatesWithoutError() {
        TriFunction<String, String, String, Exception> createException = (key, value1, value2) -> new Exception();
        var values = List.of(
            Pair.of("a", "b"),
            Pair.of("b", "c"),
            Pair.of("c", "d"),
            Pair.of("d", "d")
        );

        var result = values.stream().collect(toMapFailOnDuplicates(createException));
        var expected = Result.of(values.stream().collect(Collectors.toMap(Pair::left, Pair::right)));

        assertEquals(expected, result);
    }

    @Test
    void toMapIgnoreDuplicates() {
        TriFunction<String, String, String, Exception> createException = (key, value1, value2) -> new Exception();
        var values = Stream.of(
            Map.entry("a", "b"),
            Map.entry("a", "c"),
            Map.entry("b", "c"),
            Map.entry("c", "d"),
            Map.entry("a", "d")
        );

        var result = values.collect(Utils.toMapIgnoreDuplicates());
        var expected = Map.of(
            "a", "b",
            "b", "c",
            "c", "d"
        );

        assertEquals(expected, result);
    }

    @Test
    void allMatchTrue() {
        var stream = Stream.of("a", "ab", "ac", "a", "adle");
        var result = stream.collect(Utils.allMatch(it -> it.startsWith("a")));
        assertTrue(result);
    }

    @Test
    void allMatchFalse() {
        var stream = Stream.of("a", "ab", "ac", "caa", "a", "adle");
        var result = stream.collect(Utils.allMatch(it -> it.startsWith("a")));
        assertFalse(result);
    }
}
