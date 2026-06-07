package com.example.ragbackend.evaluation.metric;

import java.util.List;

public record RetrievalMetricResult(
        int questionCount,
        double recallAtK,
        double hitRateAtK,
        double mrr,
        List<QuestionMetricResult> questionResults
) {
}
