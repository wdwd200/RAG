package com.example.ragbackend.evaluation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EvaluationQuestionResultResponse(
        Long id,
        Long reportId,
        Long questionId,
        String question,
        List<Long> expectedChunkIds,
        List<Long> retrievedChunkIds,
        Boolean hit,
        Double reciprocalRank,
        Double recallAtK,
        Integer rankedHitPosition,
        LocalDateTime createdAt
) {
}
