package com.example.ragbackend.evaluation.dto;

import java.util.List;

public record EvaluationBadCaseResponse(
        Long questionId,
        String question,
        List<Long> expectedChunkIds,
        List<Long> retrievedChunkIds,
        Boolean hit,
        Double recallAtK,
        Double reciprocalRank,
        Integer rankedHitPosition,
        String failureReason
) {
}
