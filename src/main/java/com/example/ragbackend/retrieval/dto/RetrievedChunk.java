package com.example.ragbackend.retrieval.dto;

public record RetrievedChunk(
        Long chunkId,
        Long documentId,
        Long knowledgeBaseId,
        Integer chunkIndex,
        Double score,
        String content
) {
}
