package com.example.ragbackend.audit.dto;

import java.time.LocalDateTime;

public record LlmCallLogResponse(
        Long id,
        String requestId,
        Long sessionId,
        Long messageId,
        Long knowledgeBaseId,
        String provider,
        String modelName,
        Integer promptTokens,
        Integer completionTokens,
        Long latencyMs,
        Boolean success,
        String errorMessage,
        LocalDateTime createdAt
) {
}
