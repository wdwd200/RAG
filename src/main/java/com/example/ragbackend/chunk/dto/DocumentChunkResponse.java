package com.example.ragbackend.chunk.dto;

import java.time.LocalDateTime;

public record DocumentChunkResponse(
        Long id,
        Long knowledgeBaseId,
        Long documentId,
        Integer chunkIndex,
        String content,
        String contentHash,
        Integer processingVersion,
        Boolean isActive,
        Integer tokenCount,
        String vectorId,
        Integer pageNumber,
        String metadataJson,
        LocalDateTime createdAt
) {
}
