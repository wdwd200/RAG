package com.example.ragbackend.evaluation.dto;

import java.time.LocalDateTime;

public record EvaluationDatasetResponse(
        Long id,
        String name,
        Long knowledgeBaseId,
        String description,
        Integer questionCount,
        LocalDateTime createdAt
) {
}
