package com.example.ragbackend.evaluation.metric;

import com.example.ragbackend.common.exception.BusinessException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RetrievalMetricsCalculator {

    private static final String METRICS_REQUEST_INVALID_CODE = "EVALUATION_METRICS_REQUEST_INVALID";

    public QuestionMetricResult calculateQuestion(
            Long questionId,
            List<Long> expectedChunkIds,
            List<Long> retrievedChunkIds
    ) {
        validateQuestionInput(questionId, expectedChunkIds, retrievedChunkIds);

        Set<Long> expected = new LinkedHashSet<>(expectedChunkIds);
        Set<Long> retrievedUnique = new LinkedHashSet<>(retrievedChunkIds);

        long hitCount = retrievedUnique.stream()
                .filter(expected::contains)
                .count();
        double recallAtK = (double) hitCount / expected.size();

        Integer rankedHitPosition = findRankedHitPosition(expected, retrievedChunkIds);
        double reciprocalRank = rankedHitPosition == null ? 0.0d : 1.0d / rankedHitPosition;

        return new QuestionMetricResult(
                questionId,
                List.copyOf(expectedChunkIds),
                List.copyOf(retrievedChunkIds),
                rankedHitPosition != null,
                reciprocalRank,
                recallAtK,
                rankedHitPosition
        );
    }

    public RetrievalMetricResult summarize(List<QuestionMetricResult> questionResults) {
        if (questionResults == null || questionResults.isEmpty()) {
            throw new BusinessException(METRICS_REQUEST_INVALID_CODE, "Question metric results cannot be empty");
        }
        if (questionResults.stream().anyMatch(result -> result == null || result.expectedChunkIds().isEmpty())) {
            throw new BusinessException(METRICS_REQUEST_INVALID_CODE, "Question metric result is invalid");
        }

        int questionCount = questionResults.size();
        double recallAtK = questionResults.stream()
                .mapToDouble(QuestionMetricResult::recallAtK)
                .average()
                .orElse(0.0d);
        double hitRateAtK = (double) questionResults.stream()
                .filter(QuestionMetricResult::hit)
                .count() / questionCount;
        double mrr = questionResults.stream()
                .mapToDouble(QuestionMetricResult::reciprocalRank)
                .average()
                .orElse(0.0d);

        return new RetrievalMetricResult(questionCount, recallAtK, hitRateAtK, mrr, List.copyOf(questionResults));
    }

    private void validateQuestionInput(
            Long questionId,
            List<Long> expectedChunkIds,
            List<Long> retrievedChunkIds
    ) {
        if (questionId == null || questionId <= 0) {
            throw new BusinessException(METRICS_REQUEST_INVALID_CODE, "Question id must be positive");
        }
        if (expectedChunkIds == null || expectedChunkIds.isEmpty()) {
            throw new BusinessException(METRICS_REQUEST_INVALID_CODE, "Expected chunk ids cannot be empty");
        }
        if (expectedChunkIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(METRICS_REQUEST_INVALID_CODE, "Expected chunk ids must be positive");
        }
        if (retrievedChunkIds == null) {
            throw new BusinessException(METRICS_REQUEST_INVALID_CODE, "Retrieved chunk ids cannot be null");
        }
        if (retrievedChunkIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(METRICS_REQUEST_INVALID_CODE, "Retrieved chunk ids must be positive");
        }
    }

    private Integer findRankedHitPosition(Set<Long> expectedChunkIds, List<Long> retrievedChunkIds) {
        for (int index = 0; index < retrievedChunkIds.size(); index++) {
            if (expectedChunkIds.contains(retrievedChunkIds.get(index))) {
                return index + 1;
            }
        }
        return null;
    }
}
