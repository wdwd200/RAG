package com.example.ragbackend.evaluation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EvaluationQuestionResponse(
        Long id,
        Long datasetId,
        String question,
        String groundTruthAnswer,
        List<Long> relevantChunkIds,
        List<String> relevantContentHashes,
        Integer documentProcessingVersion,
        String questionType,
        LocalDateTime createdAt
) {
}
