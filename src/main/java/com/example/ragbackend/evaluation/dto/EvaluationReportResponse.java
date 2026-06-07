package com.example.ragbackend.evaluation.dto;

import java.time.LocalDateTime;

public record EvaluationReportResponse(
        Long id,
        Long datasetId,
        Long knowledgeBaseId,
        Integer topK,
        Integer questionCount,
        Double recallAtK,
        Double hitRateAtK,
        Double mrr,
        String status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime finishedAt,
        Integer questionResultCount
) {
}
