package com.example.ragbackend.evaluation.metric;

import java.util.List;

public record QuestionMetricResult(
        Long questionId,
        List<Long> expectedChunkIds,
        List<Long> retrievedChunkIds,
        boolean hit,
        double reciprocalRank,
        double recallAtK,
        Integer rankedHitPosition
) {
}
