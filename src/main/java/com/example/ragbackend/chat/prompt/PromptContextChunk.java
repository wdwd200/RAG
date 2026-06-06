package com.example.ragbackend.chat.prompt;

public record PromptContextChunk(
        Long chunkId,
        Long documentId,
        String fileName,
        Double score,
        String content
) {
}
