
package tools.vitruv.neojoin.utils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.vitruv.neojoin.utils.Enumerated.enumerate;
import static tools.vitruv.neojoin.utils.Enumerated.enumerated;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class EnumeratedTest {

    @Test
    public void testEnumerateEmptyIterable() {
        var iter = enumerate(List.of()).iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    public void testEnumerateEmptyStream() {
        var iter = enumerate(Stream.of()).iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    public void testEnumerateList() {
        var list = List.of("a", "b", "c", "d", "e", "f", "g", "h");
        var iter = enumerate(list).iterator();

        for (int i = 0; i < list.size(); i++) {
            assertTrue(iter.hasNext());
            var next = iter.next();
            assertEquals(list.get(i), next.value());
            assertEquals(i, next.index());
        }

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    public void testEnumerateStream() {
        var list = List.of("a", "b", "c", "d", "e", "f", "g", "h");
        var stream = list.stream();
        var iter = enumerate(stream).iterator();

        for (int i = 0; i < list.size(); i++) {
            assertTrue(iter.hasNext());
            var next = iter.next();
            assertEquals(list.get(i), next.value());
            assertEquals(i, next.index());
        }

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    public void testUseEnumeratedToAssociateElementsOfStreamWithIndices() {
        var list = List.of("a", "b", "c", "d", "e", "f", "g", "h");
        Supplier<Stream<Enumerated.IndexedValue<String>>> stream = () -> list.stream().map(enumerated());

        assertEquals(stream.get().count(), list.size());
        stream.get().forEach((x) ->
            assertEquals(list.get(x.index()), x.value())
        );
    }
}
