package com.example.ragbackend.vector.model;

public record VectorSearchResult(
        String pointId,
        Long chunkId,
        Long documentId,
        Long knowledgeBaseId,
        Integer chunkIndex,
        String contentHash,
        Integer processingVersion,
        Double score
) {
}
