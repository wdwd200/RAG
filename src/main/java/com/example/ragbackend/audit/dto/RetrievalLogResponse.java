package com.example.ragbackend.audit.dto;

import java.time.LocalDateTime;

public record RetrievalLogResponse(
        Long id,
        String requestId,
        Long sessionId,
        Long messageId,
        Long knowledgeBaseId,
        String question,
        String retrieverType,
        Integer topK,
        Long chunkId,
        Long documentId,
        Integer rankPosition,
        Double score,
        LocalDateTime createdAt
) {
}
