package tools.vitruv.neojoin.common;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UtilsTest {

    @Test
    void mapCollectorThrowsOrKeepsTheFirstValueOnDuplicates() {
        var entries = List.of(Map.entry("a", 1), Map.entry("a", 2), Map.entry("b", 3));

        assertThat(entries.stream().collect(Utils.mapCollector(false))).containsOnly(
            Map.entry("a", 1),
            Map.entry("b", 3)
        );
        assertThatThrownBy(() -> entries.stream().collect(Utils.mapCollector(true)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removeSuffixOnlyRemovesAMatchingSuffix() {
        assertThat(Utils.removeSuffix("query.nj", ".nj")).isEqualTo("query");
        assertThat(Utils.removeSuffix("query.txt", ".nj")).isEqualTo("query.txt");
    }

    @Test
    void streamOfIteratorAndIndexOf() {
        assertThat(Utils.streamOf(List.of(1, 2, 3).iterator()).toList()).containsExactly(1, 2, 3);
        assertThat(Utils.indexOf(Stream.of("a", "b"), "b")).isEqualTo(1);
        assertThat(Utils.indexOf(Stream.of("a", "b"), "c")).isEqualTo(-1);
    }

    @Test
    void getAtReturnsTheElementOrFailsOutOfBounds() {
        assertThat(Utils.getAt(Stream.of("a", "b"), 1)).isEqualTo("b");
        var stream = Stream.of("a");
        assertThatThrownBy(() -> Utils.getAt(stream, 1)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void defaultIfEmptyOnlyUsesTheDefaultForEmptyStreams() {
        assertThat(Utils.defaultIfEmpty(Stream.of(1, 2), () -> 0).toList()).containsExactly(1, 2);
        assertThat(Utils.defaultIfEmpty(Stream.<Integer>empty(), () -> 0).toList())
            .containsExactly(0);
    }

    @Test
    void reversedStreamReturnsTheElementsInReverseOrder() {
        assertThat(Utils.reversedStream(List.of(1, 2, 3)).toList()).containsExactly(3, 2, 1);
    }

}
