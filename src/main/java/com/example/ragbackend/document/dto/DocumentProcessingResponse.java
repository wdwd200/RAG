package com.example.ragbackend.document.dto;

public record DocumentProcessingResponse(
        Long documentId,
        String status,
        Integer chunkCount,
        Integer processingVersion
) {
}
