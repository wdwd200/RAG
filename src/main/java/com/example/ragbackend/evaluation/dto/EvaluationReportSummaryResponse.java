package com.example.ragbackend.evaluation.dto;

import java.time.LocalDateTime;

public record EvaluationReportSummaryResponse(
        Long reportId,
        Long datasetId,
        Long knowledgeBaseId,
        Integer topK,
        Integer questionCount,
        Double recallAtK,
        Double hitRateAtK,
        Double mrr,
        Integer badCaseCount,
        Integer noHitCount,
        Integer lowRecallCount,
        Integer lowRankCount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime finishedAt
) {
}
