package com.example.ragbackend.knowledge.dto;

import java.time.LocalDateTime;

public record KnowledgeBaseResponse(
        Long id,
        String name,
        String description,

        Long ownerId,
        String visibility,

        Integer documentCount,
        Integer chunkCount,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
