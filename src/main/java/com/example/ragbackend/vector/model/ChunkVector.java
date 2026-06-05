package com.example.ragbackend.vector.model;

import java.util.List;

public record ChunkVector(
        Long chunkId,
        Long documentId,
        Long knowledgeBaseId,
        Integer chunkIndex,
        String contentHash,
        Integer processingVersion,
        List<Float> vector
) {
}
