package com.example.ragbackend.splitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ragbackend.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class FixedWindowTextSplitterTest {

    private final FixedWindowTextSplitter textSplitter = new FixedWindowTextSplitter();

    @Test
    void splitsTextByChunkSizeAndOverlap() {
        List<TextChunk> chunks = textSplitter.split(
                "abcdefghijklmnopqrst",
                new SplitOptions(10, 2)
        );

        assertThat(chunks)
                .extracting(TextChunk::content)
                .containsExactly("abcdefghij", "ijklmnopqr", "qrst");
        assertThat(chunks)
                .extracting(TextChunk::index)
                .containsExactly(0, 1, 2);
    }

    @Test
    void returnsEmptyListWhenTextIsBlank() {
        List<TextChunk> chunks = textSplitter.split("   ", new SplitOptions(10, 2));

        assertThat(chunks).isEmpty();
    }

    @Test
    void failsWhenOverlapIsNotSmallerThanChunkSize() {
        assertThatThrownBy(() -> textSplitter.split("hello", new SplitOptions(10, 10)))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("INVALID_CHUNK_OPTIONS"));
    }
}
