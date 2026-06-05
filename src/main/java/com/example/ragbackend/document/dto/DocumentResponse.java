package com.example.ragbackend.document.dto;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        Long knowledgeBaseId,
        String fileName,
        String fileType,
        Long fileSize,
        String storagePath,
        String status,
        Integer chunkCount,
        Integer processingVersion,
        String failedStage,
        String errorMessage,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
