package com.example.ragbackend.evaluation.dto;

import java.util.List;

public record EvaluationQuestionResultCreateRequest(
        Long questionId,
        String question,
        List<Long> expectedChunkIds,
        List<Long> retrievedChunkIds,
        Boolean hit,
        Double reciprocalRank,
        Double recallAtK,
        Integer rankedHitPosition
) {
}
