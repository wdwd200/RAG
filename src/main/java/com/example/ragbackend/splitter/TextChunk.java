package com.example.ragbackend.splitter;

public record TextChunk(
        int index,
        String content,
        int tokenCount
) {
}
